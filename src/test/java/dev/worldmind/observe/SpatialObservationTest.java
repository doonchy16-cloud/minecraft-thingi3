package dev.worldmind.observe;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.region.*;
import org.junit.jupiter.api.Test;

class SpatialObservationTest {
    @Test void distantSamplesUpdateDifferentMicroCells() {
        RegionKey key = new RegionKey("minecraft:overworld",0,0);
        RegionState region = RegionState.fresh(key, 0L);
        RegionSignalInterpreter.applySpatial(region, 8, 8,
                new SpatialObservationSample(1,1,0,0,0,0,0,0), 100L);
        RegionSignalInterpreter.applySpatial(region, 120, 120,
                new SpatialObservationSample(0,0,1,0,0,0,0,0), 100L);
        assertTrue(region.spatial().cell(0,0).signal(SpatialSignal.FOREST) > 0);
        assertTrue(region.spatial().cell(7,7).signal(SpatialSignal.FARM) > 0);
        assertEquals(0.0, region.spatial().cell(0,0).signal(SpatialSignal.FARM), 1e-9);
    }
}
