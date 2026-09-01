package dev.worldmind.region;

public record SpatialProcessPlan(SpatialProcess process, int[] cells, int targetMutations) {
    public SpatialProcessPlan {
        process = process == null ? SpatialProcess.NONE : process;
        cells = cells == null ? new int[0] : cells.clone();
        targetMutations = Math.max(0, targetMutations);
    }
    @Override public int[] cells() { return cells.clone(); }
}
