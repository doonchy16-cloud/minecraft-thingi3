package dev.worldmind.protect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ProtectionIndex {
    private final List<ProtectionRecord> records;

    public ProtectionIndex(Collection<ProtectionRecord> records) {
        this.records = new ArrayList<>(records == null ? List.of() : records);
    }

    public boolean isProtected(String dimension, int x, int y, int z) {
        for (ProtectionRecord record : records) {
            if (!record.dimension().equals(dimension)) continue;
            long dx = (long) record.x() - x;
            long dy = (long) record.y() - y;
            long dz = (long) record.z() - z;
            long r = record.radius();
            if (dx * dx + dy * dy + dz * dz <= r * r) return true;
        }
        return false;
    }

    public List<ProtectionRecord> records() { return List.copyOf(records); }
}
