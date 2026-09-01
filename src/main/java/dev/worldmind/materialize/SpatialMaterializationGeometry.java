package dev.worldmind.materialize;

import dev.worldmind.region.RegionKey;
import dev.worldmind.region.SpatialField;
import dev.worldmind.region.SpatialProcess;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure deterministic geometry for converting spatial process cells into block-column targets. */
public final class SpatialMaterializationGeometry {
    private SpatialMaterializationGeometry() {}

    public static List<XZ> targets(RegionKey region, SpatialProcess process, int[] cells, long seed, int targetMutations) {
        if (cells == null || cells.length == 0 || targetMutations <= 0 || process == SpatialProcess.NONE) return List.of();
        return process == SpatialProcess.ROUTE_CORRIDOR
                ? corridor(region, cells, Math.max(8, targetMutations))
                : patches(region, cells, seed, Math.max(8, targetMutations));
    }

    private static List<XZ> patches(RegionKey region, int[] cells, long seed, int targetMutations) {
        SpatialField field = new SpatialField();
        int pointBudget = Math.max(cells.length * 8, Math.min(targetMutations / 4 + 8, cells.length * 48));
        Set<XZ> out = new LinkedHashSet<>();
        int perCell = Math.max(8, (int)Math.ceil(pointBudget / (double)cells.length));
        for (int cell : cells) {
            int minX = region.minBlockX() + field.x(cell) * SpatialField.MICRO_SIZE_BLOCKS;
            int minZ = region.minBlockZ() + field.z(cell) * SpatialField.MICRO_SIZE_BLOCKS;
            int centerX = minX + SpatialField.MICRO_SIZE_BLOCKS / 2;
            int centerZ = minZ + SpatialField.MICRO_SIZE_BLOCKS / 2;
            for (int i = 0; i < perCell; i++) {
                long h = mix(seed ^ (cell * 0x9e3779b97f4a7c15L) ^ i);
                double angle = ((h >>> 11) & 0xffff) / 65535.0 * Math.PI * 2.0;
                double radius = Math.sqrt(((h >>> 29) & 0xffff) / 65535.0) * (SpatialField.MICRO_SIZE_BLOCKS / 2.0 - 1.0);
                int x = clamp(centerX + (int)Math.round(Math.cos(angle) * radius), minX, minX + SpatialField.MICRO_SIZE_BLOCKS - 1);
                int z = clamp(centerZ + (int)Math.round(Math.sin(angle) * radius), minZ, minZ + SpatialField.MICRO_SIZE_BLOCKS - 1);
                out.add(new XZ(x,z));
            }
        }
        return List.copyOf(out);
    }

    private static List<XZ> corridor(RegionKey region, int[] cells, int targetMutations) {
        SpatialField field = new SpatialField();
        List<XZ> centers = new ArrayList<>();
        for (int cell : cells) centers.add(new XZ(field.centerBlockX(region, cell), field.centerBlockZ(region, cell)));
        if (centers.size() == 1) return List.of(centers.get(0));

        List<XZ> ordered = orderNearest(centers);
        List<XZ> out = new ArrayList<>();
        for (int i = 1; i < ordered.size(); i++) {
            XZ a = ordered.get(i - 1), b = ordered.get(i);
            int dx = b.x() - a.x(), dz = b.z() - a.z();
            int distance = Math.max(Math.abs(dx), Math.abs(dz));
            int steps = Math.max(1, (int)Math.ceil(distance / 2.0));
            for (int s = 0; s <= steps; s++) {
                if (out.size() >= targetMutations) return List.copyOf(out);
                double t = s / (double)steps;
                XZ p = new XZ((int)Math.round(a.x() + dx * t), (int)Math.round(a.z() + dz * t));
                if (out.isEmpty() || !out.get(out.size()-1).equals(p)) out.add(p);
            }
        }
        return List.copyOf(out);
    }

    private static List<XZ> orderNearest(List<XZ> points) {
        List<XZ> remaining = new ArrayList<>(points);
        remaining.sort((a,b) -> a.x() != b.x() ? Integer.compare(a.x(), b.x()) : Integer.compare(a.z(), b.z()));
        List<XZ> ordered = new ArrayList<>();
        ordered.add(remaining.remove(0));
        while (!remaining.isEmpty()) {
            XZ last = ordered.get(ordered.size() - 1);
            int best = 0;
            long bestDist = Long.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                XZ p = remaining.get(i);
                long d = Math.abs((long)p.x() - last.x()) + Math.abs((long)p.z() - last.z());
                if (d < bestDist) { bestDist = d; best = i; }
            }
            ordered.add(remaining.remove(best));
        }
        return ordered;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }

    public record XZ(int x, int z) {}
}
