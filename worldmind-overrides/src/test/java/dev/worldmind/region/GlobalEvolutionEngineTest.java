package dev.worldmind.region;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GlobalEvolutionEngineTest {
    @Test void sameSnapshotAndSeedIsDeterministic() {
        RegionState r = RegionState.fresh(new RegionKey("minecraft:overworld", 1, 2), 0);
        r.observeDisturbance(.9); r.observeTravel(.8); r.observeFarmAbandonment(.6);
        RegionalSnapshot s = r.snapshot(24000L * 20);
        GlobalEvolutionEngine e = new GlobalEvolutionEngine();
        assertEquals(e.decide(s, 42L), e.decide(s, 42L));
    }

    @Test void fullProtectionSuppressesPhysicalMutationButNotHistoryDecision() {
        RegionState r = RegionState.fresh(new RegionKey("minecraft:overworld", 0, 0), 0);
        r.observeDisturbance(1); r.setProtectionCoverage(1);
        RegionalDecision d = new GlobalEvolutionEngine().decide(r.snapshot(24000L * 30), 3L);
        assertFalse(d.physicalEligible());
    }

    @Test void repeatedTravelBuildsRoutePressure() {
        RegionState r = RegionState.fresh(new RegionKey("minecraft:overworld", 0, 0), 0);
        for (int i = 0; i < 10; i++) r.observeTravel(.5);
        assertTrue(r.pressures().routeWear() > .9);
    }

    @Test void physicalEvolutionAgeAccumulatesAcrossSchedulerEvaluations() {
        RegionState r = RegionState.fresh(new RegionKey("minecraft:overworld", 0, 0), 0);
        r.ageTo(2400L);
        assertEquals(0.2, r.snapshot(4800L).elapsedDays(), 1.0e-9);
    }
}
