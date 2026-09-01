package dev.worldmind.observe;

import dev.worldmind.civ.CivilizationState;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.content.WorldmindBlocks;
import dev.worldmind.protect.ProtectionRecord;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionState;
import dev.worldmind.state.WorldmindState;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Samples only already-loaded player surroundings; never loads remote chunks. */
public final class RegionalObserver {
    private final PlayerMovementTracker movementTracker = new PlayerMovementTracker();
    public int observe(ServerPlayer player, WorldmindState world, WorldmindConfig config) {
        ServerLevel level = player.level();
        String dimension = level.dimension().identifier().toString();
        long tick = world.worldTicks();
        int requested = config.observationSamplesPerCycle();
        SplittableRandom random = new SplittableRandom(player.getUUID().getMostSignificantBits() ^ tick / 200L);
        List<Sample> samples = new ArrayList<>(requested);

        BlockPos playerPos = player.blockPosition();
        // Always register the player's current cell even if a sample misses it.
        RegionState current = world.getOrCreateRegion(RegionKey.fromBlock(dimension, playerPos.getX(), playerPos.getZ()));
        current.markObserved(tick);
        for (PlayerMovementTracker.MovementPoint point : movementTracker.record(player.getUUID(), dimension, playerPos.getX(), playerPos.getZ())) {
            RegionKey routeKey = RegionKey.fromBlock(dimension, point.x(), point.z());
            RegionState routeRegion = world.getOrCreateRegion(routeKey);
            routeRegion.observeTravel(0.035);
            routeRegion.spatial().addAtBlock(routeKey, point.x(), point.z(), dev.worldmind.region.SpatialSignal.ROUTE, 0.075);
            routeRegion.spatial().cell(routeRegion.spatial().cellXForBlock(routeKey, point.x()), routeRegion.spatial().cellZForBlock(routeKey, point.z())).markTouched(tick);
        }

        for (int i = 0; i < requested; i++) {
            int x = playerPos.getX() + random.nextInt(-56, 57);
            int z = playerPos.getZ() + random.nextInt(-56, 57);
            BlockPos check = new BlockPos(x, playerPos.getY(), z);
            if (!level.hasChunkAt(check)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos)) continue;
            samples.add(classify(level, pos));
        }

        var byRegion = new java.util.LinkedHashMap<RegionKey, Accumulator>();
        for (Sample s : samples) {
            RegionKey key = RegionKey.fromBlock(dimension, s.pos().getX(), s.pos().getZ());
            RegionState region = world.getOrCreateRegion(key);
            RegionSignalInterpreter.applySpatial(region, s.pos().getX(), s.pos().getZ(),
                    new SpatialObservationSample(s.vegetation(), s.logsLeaves(), s.farmland(), s.village(),
                            s.path(), s.build(), s.disturbance(), s.seal()), tick);
            byRegion.computeIfAbsent(key, k -> new Accumulator()).add(s);
        }
        for (var entry : byRegion.entrySet()) {
            RegionState region = world.getOrCreateRegion(entry.getKey());
            RegionSignalInterpreter.apply(region, entry.getValue().observation(), tick);
            if (region.villagePresence() >= 0.20 && region.civilizationId() == null) {
                CivilizationState civ = CivilizationState.found(
                        "Settlement " + region.key().cellX() + ":" + region.key().cellZ(),
                        dimension, 24 + Math.max(0, entry.getValue().villageMarkers * 2), tick);
                civ.addSettlement(region.key().stableId());
                world.upsertCivilization(civ);
                region.setCivilizationId(civ.id());
                world.history().record("civilization:founding", tick, .6, List.of(), List.of(civ.id(), region.key().stableId()));
            }
        }
        refreshProtectionsNearPlayer(level, playerPos, world, config.worldsealRadius());
        return samples.size();
    }

    private static Sample classify(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int vegetation = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PODZOL) ? 1 : 0;
        int logsLeaves = state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) ? 1 : 0;
        int farmland = state.is(Blocks.FARMLAND) ? 1 : 0;
        int village = (state.getBlock() instanceof BedBlock) || state.is(Blocks.BELL) || state.is(Blocks.COMPOSTER) ? 1 : 0;
        int path = state.is(Blocks.DIRT_PATH) ? 1 : 0;
        int build = state.is(BlockTags.PLANKS) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.BRICKS) || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) ? 1 : 0;
        int disturbance = state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.NETHERRACK) ? 1 : 0;
        int seal = state.is(WorldmindBlocks.WORLDSEAL) ? 1 : 0;
        if (seal == 0) {
            BlockPos above = pos.above();
            if (level.hasChunkAt(above) && level.getBlockState(above).is(WorldmindBlocks.WORLDSEAL)) seal = 1;
        }
        return new Sample(pos, vegetation, logsLeaves, farmland, village, path, build, disturbance, seal);
    }

    private static void refreshProtectionsNearPlayer(ServerLevel level, BlockPos player, WorldmindState state, int radius) {
        List<ProtectionRecord> records = new ArrayList<>();
        for (ProtectionRecord r : state.protections()) {
            if (!r.dimension().equals(level.dimension().identifier().toString())) { records.add(r); continue; }
            BlockPos p = new BlockPos(r.x(), r.y(), r.z());
            long dx = (long)p.getX() - player.getX(), dz = (long)p.getZ() - player.getZ();
            if (dx*dx + dz*dz > 192L*192L || !level.hasChunkAt(p)) { records.add(r); continue; }
            if (level.getBlockState(p).is(WorldmindBlocks.WORLDSEAL)) records.add(r);
        }
        // Find seals in a small exact cube around the player; this is intentionally bounded.
        int scan = 24;
        for (int dx=-scan; dx<=scan; dx+=4) for (int dz=-scan; dz<=scan; dz+=4) for (int dy=-8; dy<=8; dy+=4) {
            BlockPos p = player.offset(dx,dy,dz);
            if (!level.hasChunkAt(p)) continue;
            if (level.getBlockState(p).is(WorldmindBlocks.WORLDSEAL)) {
                ProtectionRecord candidate = new ProtectionRecord(level.dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), radius);
                if (!records.contains(candidate)) records.add(candidate);
            }
        }
        state.replaceProtections(records);
    }

    private record Sample(BlockPos pos,int vegetation,int logsLeaves,int farmland,int village,int path,int build,int disturbance,int seal) {}
    private static final class Accumulator {
        int vegetation,logsLeaves,farmland,villageMarkers,path,build,disturbance,seals,samples;
        void add(Sample s){vegetation+=s.vegetation;logsLeaves+=s.logsLeaves;farmland+=s.farmland;villageMarkers+=s.village;path+=s.path;build+=s.build;disturbance+=s.disturbance;seals+=s.seal;samples++;}
        RegionalObservation observation(){return new RegionalObservation(vegetation,logsLeaves,farmland,villageMarkers,path,build,disturbance,seals,samples);}
    }
}
