package dev.worldmind.protect;

public record ProtectionRecord(String dimension, int x, int y, int z, int radius) {
    public ProtectionRecord {
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension");
        radius = Math.max(1, radius);
    }
}
