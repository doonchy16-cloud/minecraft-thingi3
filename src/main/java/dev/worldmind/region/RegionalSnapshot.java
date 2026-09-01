package dev.worldmind.region;

public record RegionalSnapshot(
        RegionKey key,
        double elapsedDays,
        double protectionCoverage,
        EvolutionPressures pressures,
        long knownSinceTick,
        long lastEvaluationTick,
        long lastMaterializationTick) {}
