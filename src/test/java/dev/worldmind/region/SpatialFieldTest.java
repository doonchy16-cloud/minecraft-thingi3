package dev.worldmind.region;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SpatialFieldTest {
    @Test void mapsBlockCoordinatesIntoEightByEightCells() {
        RegionKey key = new RegionKey("minecraft:overworld", 0, 0);
        SpatialField field = new SpatialField();
        assertEquals(0, field.cellXForBlock(key, 0));
        assertEquals(7, field.cellXForBlock(key, 127));
        assertEquals(0, field.cellZForBlock(key, 0));
        assertEquals(7, field.cellZForBlock(key, 127));
    }

    @Test void signalsAreBoundedAndNeighborsAreCardinal() {
        SpatialField field = new SpatialField();
        field.add(3, 3, SpatialSignal.FOREST, 3.0);
        assertEquals(1.0, field.cell(3,3).signal(SpatialSignal.FOREST), 1e-9);
        assertEquals(4, field.cardinalNeighbors(3,3).size());
        assertEquals(2, field.cardinalNeighbors(0,0).size());
    }

    @Test void regionLazilyInitializesSpatialFieldForLegacySaves() {
        RegionState state = RegionState.fresh(new RegionKey("minecraft:overworld", 0, 0), 0L);
        assertNotNull(state.spatial());
        state.spatial().add(1, 1, SpatialSignal.VEGETATION, .5);
        assertEquals(.5, state.spatial().cell(1,1).signal(SpatialSignal.VEGETATION), 1e-9);
    }
}
