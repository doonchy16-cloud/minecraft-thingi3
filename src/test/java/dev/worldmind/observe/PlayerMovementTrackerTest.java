package dev.worldmind.observe;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerMovementTrackerTest {
    @Test void stationaryPresenceDoesNotBecomeTravel() {
        PlayerMovementTracker tracker = new PlayerMovementTracker();
        UUID id = UUID.randomUUID();
        assertTrue(tracker.record(id, "minecraft:overworld", 10, 10).isEmpty());
        assertTrue(tracker.record(id, "minecraft:overworld", 10, 10).isEmpty());
        assertTrue(tracker.record(id, "minecraft:overworld", 11, 10).isEmpty());
    }

    @Test void movementRasterizesTheActualSegment() {
        PlayerMovementTracker tracker = new PlayerMovementTracker();
        UUID id = UUID.randomUUID();
        tracker.record(id, "minecraft:overworld", 0, 0);
        var points = tracker.record(id, "minecraft:overworld", 40, 0);
        assertFalse(points.isEmpty());
        assertEquals(0, points.get(0).z());
        assertEquals(0, points.get(points.size()-1).z());
        assertTrue(points.stream().anyMatch(p -> p.x() >= 16 && p.x() <= 24));
    }

    @Test void teleportsDoNotPaintGiantRoutes() {
        PlayerMovementTracker tracker = new PlayerMovementTracker();
        UUID id = UUID.randomUUID();
        tracker.record(id, "minecraft:overworld", 0, 0);
        assertTrue(tracker.record(id, "minecraft:overworld", 1000, 1000).isEmpty());
    }
}
