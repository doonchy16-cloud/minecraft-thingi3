package dev.worldmind.sim;

import dev.worldmind.anomaly.AnomalyScheduler;
import dev.worldmind.civ.CivilizationEngine;
import dev.worldmind.civ.CivilizationEvent;
import dev.worldmind.civ.CivilizationState;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.history.HistoricalEvent;
import dev.worldmind.region.GlobalEvolutionEngine;
import dev.worldmind.region.GlobalEvolutionScheduler;
import dev.worldmind.region.RegionState;
import dev.worldmind.region.RegionalDecision;
import dev.worldmind.region.RegionalOutcome;
import dev.worldmind.region.SpatialProcessPlanner;
import dev.worldmind.state.RegionTransformationPlan;
import dev.worldmind.state.WorldmindState;
import java.util.List;

public final class WorldSimulationService {
    private final WorldmindConfig config;
    private final GlobalEvolutionScheduler scheduler = new GlobalEvolutionScheduler();
    private final GlobalEvolutionEngine evolution = new GlobalEvolutionEngine();
    private final CivilizationEngine civilizations = new CivilizationEngine();
    private final AnomalyScheduler anomalies = new AnomalyScheduler();
    private final SpatialProcessPlanner spatialPlanner = new SpatialProcessPlanner();

    public WorldSimulationService(WorldmindConfig config) { this.config = config.validated(); }

    public WorldSimulationResult advance(WorldmindState state) {
        long now = state.worldTicks();
        List<RegionState> due = scheduler.selectDue(state.regions().values(), now, config.globalRegionsPerCycle());
        int plans = 0, history = 0;
        for (RegionState region : due) {
            long seed = mix(now ^ region.key().stableId().hashCode());
            RegionalDecision d = evolution.decide(region.snapshot(now), seed);
            HistoricalEvent event = state.history().record("region:" + d.outcome().name().toLowerCase(), now,
                    d.intensity(), List.of(), List.of(region.key().stableId()));
            history++;
            if (d.outcome() != RegionalOutcome.STASIS && d.physicalEligible() && !state.hasPendingPlanForRegion(region.key())) {
                var spatial = spatialPlanner.plan(region, d, seed, config.maxMutationsPerMaterialization(), now);
                state.addRegionPlan(RegionTransformationPlan.pending(region.key(), d.outcome(), event.id(), seed, d.intensity(), now, spatial));
                plans++;
            }
            if (config.anomalyIntensity() > 0 && anomalies.shouldManifest(region.pressures().anomaly() * config.anomalyIntensity(), seed ^ 0xA110A1L)
                    && !state.hasPendingPlanForRegion(region.key())) {
                HistoricalEvent ae = state.history().record("anomaly:unresolved", now, .95, List.of(event.id()), List.of(region.key().stableId()));
                var anomalyDecision = new RegionalDecision(RegionalOutcome.ANOMALY_MANIFESTATION, .9, true, "rare anomaly pressure");
                var anomalySpatial = spatialPlanner.plan(region, anomalyDecision, seed ^ 0xA110A1L, config.maxMutationsPerMaterialization(), now);
                state.addRegionPlan(RegionTransformationPlan.pending(region.key(), RegionalOutcome.ANOMALY_MANIFESTATION,
                        ae.id(), seed ^ 0xA110A1L, .9, now, anomalySpatial));
                history++; plans++;
            }
            region.ageTo(now);
        }

        int civAdvanced = 0;
        for (CivilizationState c : state.civilizations().values()) {
            double elapsedDays = Math.max(0.0, now - c.lastUpdatedTick()) / 24000.0;
            if (elapsedDays < 0.05) continue;
            var turn = civilizations.advance(c, Math.min(200.0, elapsedDays), mix(now ^ c.id().hashCode()));
            c.markUpdated(now);
            civAdvanced++;
            String previous = null;
            for (CivilizationEvent e : turn.events()) {
                HistoricalEvent ce = state.history().record("civilization:" + e.type(), now, e.significance(),
                        previous == null ? List.of() : List.of(previous), List.of(c.id()));
                previous = ce.id();
                history++;
            }
        }
        return new WorldSimulationResult(due.size(), plans, civAdvanced, history);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }
}
