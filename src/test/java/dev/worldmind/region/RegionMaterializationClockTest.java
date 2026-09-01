package dev.worldmind.region;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RegionMaterializationClockTest {
    @Test void zeroChangesDoNotResetPhysicalAge() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0), 0L);
        region.ageTo(4800L);
        region.recordMaterializationResult(0, 4800L);
        assertEquals(.2, region.snapshot(4800L).elapsedDays(), 1e-9);
    }

    @Test void actualChangesResetPhysicalAge() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0), 0L);
        region.ageTo(4800L);
        region.recordMaterializationResult(3, 4800L);
        assertEquals(0.0, region.snapshot(4800L).elapsedDays(), 1e-9);
    }
}
