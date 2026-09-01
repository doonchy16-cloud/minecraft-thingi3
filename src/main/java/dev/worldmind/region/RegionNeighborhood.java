package dev.worldmind.region;

import java.util.ArrayList;
import java.util.List;

/** Builds small deterministic neighborhoods without touching world/chunk APIs. */
public final class RegionNeighborhood {
    private RegionNeighborhood() {}

    public static List<RegionKey> square(RegionKey center, int radius) {
        if (center == null) throw new IllegalArgumentException("center");
        int r = Math.max(0, radius);
        List<RegionKey> keys = new ArrayList<>((r * 2 + 1) * (r * 2 + 1));
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                keys.add(new RegionKey(center.dimension(), center.cellX() + dx, center.cellZ() + dz));
            }
        }
        return List.copyOf(keys);
    }
}
