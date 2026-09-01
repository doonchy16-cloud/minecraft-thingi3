package dev.worldmind.command;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.region.*;
import dev.worldmind.state.WorldmindState;
import org.junit.jupiter.api.Test;

class WorldmindAdminServiceTest {
    @Test void advanceThirtyDaysMovesWorldmindChronologyAndEvaluatesKnownRegions() {
        WorldmindState state = new WorldmindState();
        RegionState region = state.getOrCreateRegion(new RegionKey("minecraft:overworld",0,0));
        region.spatial().add(3,3,SpatialSignal.FOREST,1.0);
        region.observeForest(.8);
        WorldmindAdminService service = new WorldmindAdminService();
        var result = service.advance(state, WorldmindConfig.defaults(), 30);
        assertEquals(30L*24000L, state.worldTicks());
        assertTrue(result.regionsEvaluated() >= 1);
        assertTrue(result.historyEvents() >= 1);
        assertTrue(result.regionalPlans() >= 1);
    }

    @Test void inspectReportsSpatialSignalsAndPhysicalAge() {
        WorldmindState state = new WorldmindState();
        RegionState region = state.getOrCreateRegion(new RegionKey("minecraft:overworld",0,0));
        region.spatial().add(2,2,SpatialSignal.ROUTE,.75);
        state.setWorldTicks(30L*24000L);
        String text = new WorldmindAdminService().inspect(state, "minecraft:overworld", 40, 40);
        assertTrue(text.contains("physicalAgeDays=30.00"));
        assertTrue(text.contains("route="));
    }
}
