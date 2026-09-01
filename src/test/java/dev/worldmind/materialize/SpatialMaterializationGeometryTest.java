package dev.worldmind.materialize;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.region.*;
import org.junit.jupiter.api.Test;

class SpatialMaterializationGeometryTest {
    @Test void patchTargetsStayClusteredInsideChosenMicroCells() {
        RegionKey key = new RegionKey("minecraft:overworld",0,0);
        int[] cells = { new SpatialField().index(3,3), new SpatialField().index(4,3) };
        var points = SpatialMaterializationGeometry.targets(key, SpatialProcess.FOREST_FRONTIER, cells, 44L, 120);
        assertTrue(points.size() >= 20);
        assertTrue(points.stream().allMatch(p -> p.x() >= 48 && p.x() < 80 && p.z() >= 48 && p.z() < 64));
    }

    @Test void routeCorridorContainsContinuousShortSteps() {
        RegionKey key = new RegionKey("minecraft:overworld",0,0);
        SpatialField f = new SpatialField();
        int[] cells = { f.index(1,4), f.index(2,4), f.index(3,4), f.index(4,4), f.index(5,4) };
        var points = SpatialMaterializationGeometry.targets(key, SpatialProcess.ROUTE_CORRIDOR, cells, 2L, 200);
        assertTrue(points.size() > 20);
        for (int i=1;i<points.size();i++) {
            int manhattan = Math.abs(points.get(i).x()-points.get(i-1).x()) + Math.abs(points.get(i).z()-points.get(i-1).z());
            assertTrue(manhattan <= 4, "route gap="+manhattan);
        }
    }
}
