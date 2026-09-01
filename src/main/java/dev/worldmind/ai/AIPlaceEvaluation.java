package dev.worldmind.ai;

import dev.worldmind.core.EvolutionDecision;

public record AIPlaceEvaluation(AIPlaceContext context, EvolutionDecision deterministicDecision) {
    public AIPlaceEvaluation {
        if (context == null || deterministicDecision == null) throw new IllegalArgumentException();
    }
}
