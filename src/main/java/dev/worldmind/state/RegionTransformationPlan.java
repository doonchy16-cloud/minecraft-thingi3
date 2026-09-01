package dev.worldmind.state;

import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionalOutcome;
import dev.worldmind.region.SpatialProcess;
import dev.worldmind.region.SpatialProcessPlan;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RegionTransformationPlan {
    private String id;
    private RegionKey region;
    private RegionalOutcome outcome;
    private String causeEventId;
    private long seed;
    private double intensity;
    private long createdTick;
    private PlanStatus status;
    private SpatialProcess process;
    private int[] spatialCells;
    private int targetMutations;

    private RegionTransformationPlan() {}

    private RegionTransformationPlan(String id, RegionKey region, RegionalOutcome outcome, String causeEventId,
            long seed, double intensity, long createdTick, PlanStatus status, SpatialProcess process,
            int[] spatialCells, int targetMutations) {
        this.id = id;
        this.region = region;
        this.outcome = outcome;
        this.causeEventId = causeEventId;
        this.seed = seed;
        this.intensity = Math.max(0, Math.min(1, intensity));
        this.createdTick = createdTick;
        this.status = status;
        this.process = process == null ? SpatialProcess.NONE : process;
        this.spatialCells = spatialCells == null ? new int[0] : spatialCells.clone();
        this.targetMutations = Math.max(0, targetMutations);
    }

    public static RegionTransformationPlan pending(RegionKey region, RegionalOutcome outcome, String causeEventId,
            long seed, double intensity, long createdTick) {
        return pending(region, outcome, causeEventId, seed, intensity, createdTick,
                new SpatialProcessPlan(SpatialProcess.NONE, new int[0], 0));
    }

    public static RegionTransformationPlan pending(RegionKey region, RegionalOutcome outcome, String causeEventId,
            long seed, double intensity, long createdTick, SpatialProcessPlan spatial) {
        String stable = region.stableId()+"|"+outcome+"|"+causeEventId+"|"+seed+"|"+createdTick;
        String id = UUID.nameUUIDFromBytes(stable.getBytes(StandardCharsets.UTF_8)).toString();
        return new RegionTransformationPlan(id, region, outcome, causeEventId, seed, intensity, createdTick,
                PlanStatus.PENDING, spatial.process(), spatial.cells(), spatial.targetMutations());
    }

    public String id(){return id;} public RegionKey region(){return region;} public RegionalOutcome outcome(){return outcome;}
    public String causeEventId(){return causeEventId;} public long seed(){return seed;} public double intensity(){return intensity;}
    public long createdTick(){return createdTick;} public PlanStatus status(){return status;}
    public SpatialProcess process(){return process == null ? SpatialProcess.NONE : process;}
    public int[] spatialCells(){return spatialCells == null ? new int[0] : spatialCells.clone();}
    public int targetMutations(){return Math.max(0, targetMutations);}
    public void markCommitted(){status=PlanStatus.COMMITTED;} public void markDeferred(){status=PlanStatus.DEFERRED;}
}
