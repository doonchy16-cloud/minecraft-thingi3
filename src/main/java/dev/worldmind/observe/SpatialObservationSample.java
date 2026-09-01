package dev.worldmind.observe;

public record SpatialObservationSample(
        int vegetation,
        int logsLeaves,
        int farmland,
        int village,
        int path,
        int build,
        int disturbance,
        int seal) {}
