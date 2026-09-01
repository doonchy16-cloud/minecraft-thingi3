package dev.worldmind.state;

import dev.worldmind.core.EvolutionType;
import java.util.UUID;

public final class TransformationPlan {
    private String id;
    private String placeId;
    private EvolutionType type;
    private long seed;
    private double intensity;
    private long createdTick;
    private PlanStatus status;

    private TransformationPlan() {}

    private TransformationPlan(String id, String placeId, EvolutionType type, long seed, double intensity,
            long createdTick, PlanStatus status) {
        this.id = id;
        this.placeId = placeId;
        this.type = type;
        this.seed = seed;
        this.intensity = Math.max(0.0, Math.min(1.0, intensity));
        this.createdTick = createdTick;
        this.status = status;
    }

    public static TransformationPlan pending(String placeId, EvolutionType type, long seed, double intensity, long createdTick) {
        if (placeId == null || type == null) throw new IllegalArgumentException();
        return new TransformationPlan(UUID.randomUUID().toString(), placeId, type, seed, intensity, createdTick, PlanStatus.PENDING);
    }

    public String id() { return id; }
    public String placeId() { return placeId; }
    public EvolutionType type() { return type; }
    public long seed() { return seed; }
    public double intensity() { return intensity; }
    public long createdTick() { return createdTick; }
    public PlanStatus status() { return status; }
    public void markCommitted() { status = PlanStatus.COMMITTED; }
    public void markDeferred() { status = PlanStatus.DEFERRED; }
}
