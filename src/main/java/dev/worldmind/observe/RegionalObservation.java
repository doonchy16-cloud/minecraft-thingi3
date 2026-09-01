package dev.worldmind.observe;

/** Bounded aggregate sampled only from normally loaded blocks. */
public record RegionalObservation(
        int vegetationBlocks,
        int logsLeaves,
        int farmlandBlocks,
        int villageMarkers,
        int pathBlocks,
        int playerBuildBlocks,
        int disturbanceBlocks,
        int worldseals,
        int samples) {
    public int denominator() { return Math.max(1, samples); }
}
