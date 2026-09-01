package dev.worldmind.ai;

import dev.worldmind.core.EvolutionType;

/** Compact semantic summary sent to external intelligence. Never contains chunk/block payloads. */
public record AIPlaceContext(
        String placeId,
        String placeKind,
        double placeConfidence,
        String dominantPalette,
        double defensiveIntent,
        double unfinishedIntent,
        double expansionIntent,
        double absenceDays,
        double naturePressure,
        double settlementPressure,
        double structuralFragility,
        double threatPressure,
        boolean sealed,
        EvolutionType deterministicRecommendation,
        double deterministicIntensity) {

    public AIPlaceContext {
        placeId = placeId == null ? "" : placeId;
        placeKind = placeKind == null ? "unknown" : placeKind;
        dominantPalette = dominantPalette == null ? "mixed" : dominantPalette;
        placeConfidence = clamp01(placeConfidence);
        defensiveIntent = clamp01(defensiveIntent);
        unfinishedIntent = clamp01(unfinishedIntent);
        expansionIntent = clamp01(expansionIntent);
        absenceDays = finiteNonNegative(absenceDays);
        naturePressure = clamp01(naturePressure);
        settlementPressure = clamp01(settlementPressure);
        structuralFragility = clamp01(structuralFragility);
        threatPressure = clamp01(threatPressure);
        deterministicRecommendation = deterministicRecommendation == null ? EvolutionType.STASIS : deterministicRecommendation;
        deterministicIntensity = clamp01(deterministicIntensity);
    }

    public AIPlaceContext withoutStructureDetail() {
        return new AIPlaceContext(placeId, placeKind, placeConfidence, "hidden", 0.0, 0.0, 0.0,
                absenceDays, naturePressure, settlementPressure, structuralFragility, threatPressure, sealed,
                deterministicRecommendation, deterministicIntensity);
    }

    private static double clamp01(double v) { return Double.isFinite(v) ? Math.max(0.0, Math.min(1.0, v)) : 0.0; }
    private static double finiteNonNegative(double v) { return Double.isFinite(v) ? Math.max(0.0, v) : 0.0; }
}
