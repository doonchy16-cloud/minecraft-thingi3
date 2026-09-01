package dev.worldmind.config;

import dev.worldmind.core.ChronologyMode;

public record WorldmindConfig(
        ChronologyMode chronologyMode,
        double minimumAbsenceDays,
        int simulationUnitsPerTick,
        int globalRegionsPerCycle,
        int observationSamplesPerCycle,
        int maxMutationsPerMaterialization,
        int worldsealRadius,
        double anomalyIntensity,
        int visualEffectIntensity,
        boolean debugLogging) {

    public static WorldmindConfig defaults() {
        return new WorldmindConfig(ChronologyMode.CAPPED_LIVING, 5.0, 4, 16, 64, 384, 24, 1.0, 1, false);
    }

    public WorldmindConfig validated() {
        return new WorldmindConfig(
                chronologyMode == null ? ChronologyMode.CAPPED_LIVING : chronologyMode,
                clamp(minimumAbsenceDays, 1.0, 365.0),
                clamp(simulationUnitsPerTick, 1, 128),
                clamp(globalRegionsPerCycle, 1, 256),
                clamp(observationSamplesPerCycle, 4, 512),
                clamp(maxMutationsPerMaterialization, 1, 4096),
                clamp(worldsealRadius, 4, 128),
                clamp(anomalyIntensity, 0.0, 4.0),
                clamp(visualEffectIntensity, 0, 3),
                debugLogging);
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
