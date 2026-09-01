package dev.worldmind.sim;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.worldmind.ai.AIPlaceContextBuilder;
import dev.worldmind.ai.WorldmindAIManager;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.config.WorldmindConfigLoader;
import dev.worldmind.core.CatchUpResult;
import dev.worldmind.core.ChronologyPolicy;
import dev.worldmind.diagnostic.WorldmindDiagnostics;
import dev.worldmind.materialize.RegionalMaterializer;
import dev.worldmind.materialize.WorldmindMaterializer;
import dev.worldmind.observe.ObservedPlace;
import dev.worldmind.observe.PlaceRecognizer;
import dev.worldmind.observe.RegionalObserver;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionNeighborhood;
import dev.worldmind.region.SpatialProtectionMask;
import dev.worldmind.state.PlaceRecord;
import dev.worldmind.state.RegionTransformationPlan;
import dev.worldmind.state.TransformationPlan;
import dev.worldmind.state.WorldmindSavedData;
import dev.worldmind.state.WorldmindState;

/**
 * Runtime orchestration deliberately separates:
 * loaded observation -> abstract simulation -> loaded materialization.
 * No stage is allowed to require remote chunk loading.
 */
public final class WorldmindRuntime {
    private static MinecraftServer activeServer;
    private static boolean initialized;
    private static final PlaceRecognizer PLACE_RECOGNIZER = new PlaceRecognizer();
    private static final RegionalObserver REGIONAL_OBSERVER = new RegionalObserver();

    private WorldmindRuntime() {}

    public static void tick(MinecraftServer server) {
        WorldmindConfig config = WorldmindConfigLoader.get().validated();
        WorldmindSavedData saved = WorldmindSavedData.get(server);
        WorldmindState state = saved.state();

        if (activeServer != server) {
            activeServer = server;
            initialized = false;
        }
        if (!initialized) {
            applyOfflineChronology(state, config);
            initialized = true;
            saved.changed();
        }

        long tick = state.incrementWorldTicks();

        // Physical changes are checked frequently, but only for the few regions around live players.
        if (tick % 20L == 0L) {
            materializeNearPlayers(server, state, config);
        }

        // Bounded sampling of blocks Minecraft already loaded normally.
        if (tick % 200L == 0L) {
            int samples = observePlayers(server, state, config);
            saved.changed();
            WorldmindDiagnostics.debug("observed {} loaded-world sample(s); known regions={}", samples, state.regions().size());
        }

        // One abstract evolution cycle per Minecraft minute. Each cycle has a hard region budget.
        if (tick % 1200L == 0L) {
            int aiRequests = scheduleAIAdvice(state, config);
            WorldSimulationResult global = new WorldSimulationService(config).advance(state);
            int placePlans = new PlaceSimulationService(config, WorldmindAIManager.global()).simulateDuePlaces(state);
            state.setLastRealTimeMillis(System.currentTimeMillis());
            saved.changed();
            WorldmindDiagnostics.debug(
                    "global cycle regions={} regionPlans={} civs={} history={} placePlans={} aiRequests={}",
                    global.regionsEvaluated(), global.regionalPlans(), global.civilizationsAdvanced(),
                    global.historyEvents(), placePlans, aiRequests);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        WorldmindSavedData saved = WorldmindSavedData.get(server);
        saved.state().setLastRealTimeMillis(System.currentTimeMillis());
        saved.changed();
        WorldmindAIManager.global().clearSession();
        activeServer = null;
        initialized = false;
    }

    private static void applyOfflineChronology(WorldmindState state, WorldmindConfig config) {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - state.lastRealTimeMillis());
        CatchUpResult result = new ChronologyPolicy().compute(config.chronologyMode(), elapsed);
        state.setWorldTicks(state.worldTicks() + result.abstractTicks());
        state.setLastRealTimeMillis(now);
        WorldmindDiagnostics.debug("offline catch-up: {} abstract ticks{}", result.abstractTicks(), result.capped() ? " (capped)" : "");
    }

    private static int observePlayers(MinecraftServer server, WorldmindState state, WorldmindConfig config) {
        int samples = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            samples += REGIONAL_OBSERVER.observe(player, state, config);
            observePlace(player, state);
            refreshRegionProtectionCoverage(player, state, config);
        }
        return samples;
    }

    private static void observePlace(ServerPlayer player, WorldmindState state) {
        Optional<ObservedPlace> observed = PLACE_RECOGNIZER.recognize(player);
        if (observed.isEmpty()) return;
        ObservedPlace o = observed.get();
        String dimension = player.level().dimension().identifier().toString();
        String owner = player.getUUID().toString();
        Optional<PlaceRecord> existing = state.nearestOwnedPlace(dimension, o.x(), o.y(), o.z(), owner, 18);
        PlaceRecord place;
        if (existing.isPresent()) {
            place = existing.get();
            place.markPresence(player.getUUID(), state.worldTicks());
            place.setConfidence(Math.max(place.confidence(), o.confidence()));
            place.setKind(o.kind());
            place.setSealed(o.sealed());
            place.setStructureProfile(o.structure());
        } else {
            place = PlaceRecord.create(dimension, o.x(), o.y(), o.z(), o.radius(), player.getUUID(),
                    o.confidence(), o.kind(), state.worldTicks());
            place.setSealed(o.sealed());
            place.setStructureProfile(o.structure());
            state.upsertPlace(place);
            state.getOrCreateRegion(RegionKey.fromBlock(dimension, o.x(), o.z())).observePlayerBuild(o.confidence());
            WorldmindDiagnostics.debug("recognized new {} at {},{},{} confidence={}", o.kind(), o.x(), o.y(), o.z(), o.confidence());
        }
    }

    private static void refreshRegionProtectionCoverage(ServerPlayer player, WorldmindState state, WorldmindConfig config) {
        ServerLevel level = player.level();
        String dimension = level.dimension().identifier().toString();
        BlockPos pos = player.blockPosition();
        RegionKey center = RegionKey.fromBlock(dimension, pos.getX(), pos.getZ());
        double sealArea = Math.PI * config.worldsealRadius() * config.worldsealRadius();
        double regionArea = RegionKey.CELL_SIZE_BLOCKS * (double) RegionKey.CELL_SIZE_BLOCKS;

        // Only the player's 3x3 regional neighborhood is refreshed. Never walk all known regions.
        for (RegionKey key : RegionNeighborhood.square(center, 1)) {
            state.region(key).ifPresent(region -> {
                int hits = state.protectionCount(key);
                // Physical mutation still performs exact per-block checks; this value only biases abstract evolution.
                double approximateCoverage = Math.min(0.95, hits * sealArea / regionArea);
                region.setProtectionCoverage(approximateCoverage);
                SpatialProtectionMask.apply(region, state.protections());
            });
        }
    }

    private static int scheduleAIAdvice(WorldmindState state, WorldmindConfig config) {
        WorldmindAIManager manager = WorldmindAIManager.global();
        long now = state.worldTicks();
        manager.advanceClock(now);
        var aiConfig = manager.config();
        if (!aiConfig.enabled() || !aiConfig.transformation()) return 0;

        long minTicks = Math.round(config.minimumAbsenceDays() * 24_000.0);
        int scanBudget = Math.max(4, Math.min(64, config.simulationUnitsPerTick() * 4));
        int scanned = 0, scheduled = 0;
        for (PlaceRecord place : state.places().values()) {
            if (scanned++ >= scanBudget) break;
            if (state.hasPendingPlanForPlace(place.id())) continue;
            long absence = Math.max(0L, now - place.lastPresenceTick());
            if (absence < minTicks) continue;
            if (now - place.lastEvaluationTick() < 24_000L) continue;
            var evaluation = AIPlaceContextBuilder.evaluate(place, now, config.minimumAbsenceDays());
            if (manager.consider(evaluation.context(), config.minimumAbsenceDays(), now)) scheduled++;
            if (scheduled >= aiConfig.maxConcurrentRequests()) break;
        }
        return scheduled;
    }

    public static int materializeNow(MinecraftServer server) {
        WorldmindSavedData saved = WorldmindSavedData.get(server);
        int changedPlans = materializeNearPlayers(server, saved.state(), WorldmindConfigLoader.get().validated());
        if (changedPlans > 0) saved.changed();
        return changedPlans;
    }

    private static int materializeNearPlayers(MinecraftServer server, WorldmindState state, WorldmindConfig config) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return 0;
        int appliedPlans = 0;

        RegionalMaterializer regional = new RegionalMaterializer(config);
        WorldmindMaterializer places = new WorldmindMaterializer(config);

        for (ServerPlayer player : players) {
            ServerLevel level = player.level();
            String dimension = level.dimension().identifier().toString();
            BlockPos pp = player.blockPosition();
            RegionKey center = RegionKey.fromBlock(dimension, pp.getX(), pp.getZ());

            int regionBudget = 3;
            outer:
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    RegionKey key = new RegionKey(dimension, center.cellX() + dx, center.cellZ() + dz);
                    Optional<RegionTransformationPlan> plan = state.pendingRegionPlan(key);
                    if (plan.isEmpty()) continue;
                    RegionalMaterializer.Result result = regional.apply(level, state, plan.get());
                    if (result != RegionalMaterializer.Result.DEFERRED) {
                        WorldmindDiagnostics.debug("regional materialization {} {} => {}", key.stableId(), plan.get().outcome(), result);
                        if (result == RegionalMaterializer.Result.APPLIED) appliedPlans++;
                        regionBudget--;
                    }
                    if (regionBudget <= 0) break outer;
                }
            }

            // Specialized place transformations keep the dramatic abandonment/constructive-intervention loop.
            int placeBudget = 2;
            for (TransformationPlan plan : state.plans().values()) {
                if (placeBudget <= 0) break;
                if (plan.status() != dev.worldmind.state.PlanStatus.PENDING) continue;
                PlaceRecord place = state.place(plan.placeId()).orElse(null);
                if (place == null || !place.dimension().equals(dimension)) continue;
                if (place.distanceSquared(pp.getX(), pp.getY(), pp.getZ()) > 80L * 80L) continue;
                WorldmindMaterializer.Result result = places.apply(level, place, plan);
                if (result != WorldmindMaterializer.Result.DEFERRED) {
                    if (result == WorldmindMaterializer.Result.APPLIED) appliedPlans++;
                    placeBudget--;
                    WorldmindDiagnostics.debug("place materialization {} for {} => {}", plan.type(), place.id(), result);
                }
            }
        }
        return appliedPlans;
    }
}
