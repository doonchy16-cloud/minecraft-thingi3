package dev.worldmind.state;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import dev.worldmind.observe.StructureIntentProfile;

public final class PlaceRecord {
    private String id;
    private String dimension;
    private int x;
    private int y;
    private int z;
    private int radius;
    private Set<String> ownerIds;
    private double confidence;
    private PlaceKind kind;
    private long createdTick;
    private long lastPresenceTick;
    private long lastEvaluationTick;
    private boolean sealed;
    private StructureIntentProfile structureProfile = StructureIntentProfile.unknown();

    private PlaceRecord() {}

    private PlaceRecord(String id, String dimension, int x, int y, int z, int radius, Set<String> ownerIds,
            double confidence, PlaceKind kind, long createdTick, long lastPresenceTick, long lastEvaluationTick, boolean sealed) {
        this.id = id;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(4, radius);
        this.ownerIds = new LinkedHashSet<>(ownerIds);
        this.confidence = clamp01(confidence);
        this.kind = kind == null ? PlaceKind.UNKNOWN : kind;
        this.createdTick = createdTick;
        this.lastPresenceTick = lastPresenceTick;
        this.lastEvaluationTick = lastEvaluationTick;
        this.sealed = sealed;
    }

    public static PlaceRecord create(String dimension, int x, int y, int z, int radius, UUID owner,
            double confidence, PlaceKind kind, long tick) {
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension");
        if (owner == null) throw new IllegalArgumentException("owner");
        return new PlaceRecord(UUID.randomUUID().toString(), dimension, x, y, z, radius,
                Set.of(owner.toString()), confidence, kind, tick, tick, tick, false);
    }

    public String id() { return id; }
    public String dimension() { return dimension; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public int radius() { return radius; }
    public Set<String> ownerIds() { return Set.copyOf(ownerIds); }
    public double confidence() { return confidence; }
    public PlaceKind kind() { return kind; }
    public long createdTick() { return createdTick; }
    public long lastPresenceTick() { return lastPresenceTick; }
    public long lastEvaluationTick() { return lastEvaluationTick; }
    public boolean sealed() { return sealed; }
    public StructureIntentProfile structureProfile() { return structureProfile == null ? StructureIntentProfile.unknown() : structureProfile; }

    public void markPresence(UUID player, long tick) {
        if (player != null) ownerIds.add(player.toString());
        lastPresenceTick = Math.max(lastPresenceTick, tick);
    }

    public void markEvaluated(long tick) { lastEvaluationTick = Math.max(lastEvaluationTick, tick); }
    public void setSealed(boolean value) { sealed = value; }
    public void setConfidence(double value) { confidence = clamp01(value); }
    public void setKind(PlaceKind value) { kind = value == null ? PlaceKind.UNKNOWN : value; }
    public void setStructureProfile(StructureIntentProfile value) { structureProfile = value == null ? StructureIntentProfile.unknown() : value; }

    public long distanceSquared(int px, int py, int pz) {
        long dx = (long) x - px, dy = (long) y - py, dz = (long) z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
