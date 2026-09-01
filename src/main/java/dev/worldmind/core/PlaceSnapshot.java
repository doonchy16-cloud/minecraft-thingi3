package dev.worldmind.core;

import java.util.UUID;

public record PlaceSnapshot(
        UUID placeId,
        double absenceDays,
        boolean protectedPlace,
        double confidence,
        double naturePressure,
        double settlementPressure,
        double structuralFragility,
        double threatPressure) {

    public PlaceSnapshot {
        if (placeId == null) throw new IllegalArgumentException("placeId");
        absenceDays = Math.max(0.0, absenceDays);
        confidence = clamp01(confidence);
        naturePressure = clamp01(naturePressure);
        settlementPressure = clamp01(settlementPressure);
        structuralFragility = clamp01(structuralFragility);
        threatPressure = clamp01(threatPressure);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
