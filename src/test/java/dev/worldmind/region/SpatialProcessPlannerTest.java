package dev.worldmind.region;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SpatialProcessPlannerTest {
    @Test void olderWorldProducesLargerPhysicalTarget() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0), 0L);
        region.spatial().add(3,3,SpatialSignal.FOREST,1.0);
        SpatialProcessPlanner planner = new SpatialProcessPlanner();
        RegionalDecision decision = new RegionalDecision(RegionalOutcome.VEGETATION_SPREAD,.7,true,"test");
        int five = planner.plan(region, decision, 1L, 384, 5L*24000L).targetMutations();
        int thirty = planner.plan(region, decision, 1L, 384, 30L*24000L).targetMutations();
        int hundred = planner.plan(region, decision, 1L, 384, 100L*24000L).targetMutations();
        assertTrue(five > 0);
        assertTrue(thirty > five);
        assertTrue(hundred >= thirty);
        assertTrue(hundred <= 384);
    }

    @Test void routeUsesHotRouteCellsAndForestUsesFrontierCluster() {
        RegionState region = RegionState.fresh(new RegionKey("minecraft:overworld",0,0), 0L);
        for (int x=1;x<=5;x++) region.spatial().add(x,4,SpatialSignal.ROUTE,.8);
        region.spatial().add(2,2,SpatialSignal.FOREST,1.0);
        region.spatial().add(3,2,SpatialSignal.FOREST,.8);
        SpatialProcessPlanner planner = new SpatialProcessPlanner();
        var route = planner.plan(region,new RegionalDecision(RegionalOutcome.ROUTE_FORMATION,.8,true,""),2L,384,30L*24000L);
        assertEquals(SpatialProcess.ROUTE_CORRIDOR, route.process());
        assertTrue(route.cells().length >= 3);
        var forest = planner.plan(region,new RegionalDecision(RegionalOutcome.VEGETATION_SPREAD,.8,true,""),3L,384,30L*24000L);
        assertEquals(SpatialProcess.FOREST_FRONTIER, forest.process());
        assertTrue(forest.cells().length >= 2);
    }
}
