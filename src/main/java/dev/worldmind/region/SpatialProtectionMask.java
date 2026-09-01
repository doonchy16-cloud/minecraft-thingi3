package dev.worldmind.region;

import dev.worldmind.protect.ProtectionRecord;
import java.util.Collection;

/** Conservative X/Z micro-cell mask for earned Worldseal physical domains. Exact block safety remains authoritative. */
public final class SpatialProtectionMask {
    private SpatialProtectionMask() {}

    public static void apply(RegionState region, Collection<ProtectionRecord> records) {
        SpatialField field = region.spatial();
        for (int z=0; z<SpatialField.GRID_SIZE; z++) for (int x=0; x<SpatialField.GRID_SIZE; x++)
            field.set(x,z,SpatialSignal.PROTECTION,0.0);
        if (records == null) return;
        RegionKey key = region.key();
        for (ProtectionRecord record : records) {
            if (!record.dimension().equals(key.dimension())) continue;
            for (int z=0; z<SpatialField.GRID_SIZE; z++) for (int x=0; x<SpatialField.GRID_SIZE; x++) {
                int minX = key.minBlockX() + x * SpatialField.MICRO_SIZE_BLOCKS;
                int minZ = key.minBlockZ() + z * SpatialField.MICRO_SIZE_BLOCKS;
                int maxX = minX + SpatialField.MICRO_SIZE_BLOCKS - 1;
                int maxZ = minZ + SpatialField.MICRO_SIZE_BLOCKS - 1;
                long nearX = Math.max(minX, Math.min(maxX, record.x()));
                long nearZ = Math.max(minZ, Math.min(maxZ, record.z()));
                long dx = nearX - record.x();
                long dz = nearZ - record.z();
                long r = record.radius();
                if (dx*dx + dz*dz <= r*r) field.set(x,z,SpatialSignal.PROTECTION,1.0);
            }
        }
    }
}
