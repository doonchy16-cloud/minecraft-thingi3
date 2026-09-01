package dev.worldmind.region;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.protect.ProtectionRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpatialProtectionMaskTest {
    @Test void worldsealMarksIntersectingMicroCellsButNotFarCells() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0),0);
        SpatialProtectionMask.apply(region, List.of(new ProtectionRecord("minecraft:overworld",32,64,32,24)));
        assertTrue(region.spatial().cell(2,2).signal(SpatialSignal.PROTECTION) > .9);
        assertEquals(0.0, region.spatial().cell(7,7).signal(SpatialSignal.PROTECTION), 1e-9);
    }

    @Test void plannerAvoidsProtectedCellsWhenAlternativesExist() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0),0);
        region.spatial().add(2,2,SpatialSignal.FOREST,1.0);
        region.spatial().set(2,2,SpatialSignal.PROTECTION,1.0);
        region.spatial().add(3,2,SpatialSignal.FOREST,.8);
        var plan = new SpatialProcessPlanner().plan(region,
                new RegionalDecision(RegionalOutcome.VEGETATION_SPREAD,.8,true,""), 2L,384,30L*24000L);
        for (int cell : plan.cells()) assertTrue(region.spatial().cell(region.spatial().x(cell),region.spatial().z(cell)).signal(SpatialSignal.PROTECTION) < .5);
    }
}
