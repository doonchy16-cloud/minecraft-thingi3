package dev.worldmind.ai;

import dev.worldmind.core.EvolutionType;

/** Sanitized bounded advisory data that has passed deterministic safety gates. */
public record AIValidatedAdvice(
        String placeId,
        EvolutionType recommendation,
        double confidence,
        double intensityAdjustment,
        String reason,
        String styleHint) {}
