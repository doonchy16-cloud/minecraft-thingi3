package dev.worldmind.observe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tracks sparse player movement without treating stationary presence or teleports as route usage. */
public final class PlayerMovementTracker {
    private static final int MIN_DISTANCE_SQUARED = 16;
    private static final int MAX_DISTANCE_SQUARED = 160 * 160;
    private static final int SAMPLE_STEP_BLOCKS = 4;
    private final Map<UUID, Position> last = new HashMap<>();

    public List<MovementPoint> record(UUID playerId, String dimension, int x, int z) {
        Position previous = last.put(playerId, new Position(dimension, x, z));
        if (previous == null || !previous.dimension().equals(dimension)) return List.of();
        long dx = (long)x - previous.x();
        long dz = (long)z - previous.z();
        long distanceSquared = dx * dx + dz * dz;
        if (distanceSquared < MIN_DISTANCE_SQUARED || distanceSquared > MAX_DISTANCE_SQUARED) return List.of();

        double distance = Math.sqrt(distanceSquared);
        int steps = Math.max(1, (int)Math.ceil(distance / SAMPLE_STEP_BLOCKS));
        List<MovementPoint> points = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            int px = (int)Math.round(previous.x() + dx * t);
            int pz = (int)Math.round(previous.z() + dz * t);
            MovementPoint point = new MovementPoint(px, pz);
            if (points.isEmpty() || !points.get(points.size() - 1).equals(point)) points.add(point);
        }
        return List.copyOf(points);
    }

    public record MovementPoint(int x, int z) {}
    private record Position(String dimension, int x, int z) {}
}
