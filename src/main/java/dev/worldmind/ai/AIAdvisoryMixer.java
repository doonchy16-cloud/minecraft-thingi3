package dev.worldmind.ai;

import dev.worldmind.core.EvolutionDecision;
import dev.worldmind.core.EvolutionType;
import java.util.Optional;
import java.util.Set;

public final class AIAdvisoryMixer {
    private static final Set<String> IMMUTABLE_SAFETY_REASONS = Set.of(
            "worldseal", "absence-below-threshold", "low-place-confidence");

    public EvolutionDecision mix(AIPlaceContext context, EvolutionDecision deterministic,
            Optional<AIValidatedAdvice> advice) {
        if (deterministic == null) throw new IllegalArgumentException("deterministic");
        if (context == null || context.sealed()) return deterministic;
        if (IMMUTABLE_SAFETY_REASONS.contains(deterministic.rationale())) return deterministic;
        AIValidatedAdvice a = advice == null ? null : advice.orElse(null);
        if (a == null || !context.placeId().equals(a.placeId())) return deterministic;

        EvolutionType type = a.recommendation() == null ? deterministic.type() : a.recommendation();
        double intensity = type == EvolutionType.STASIS ? 0.0
                : Math.max(0.0, Math.min(1.0, deterministic.intensity() + a.intensityAdjustment()));
        String rationale = deterministic.rationale().isEmpty() ? "ai-advisory"
                : deterministic.rationale() + "+ai-advisory";
        return new EvolutionDecision(type, intensity, deterministic.decisionSeed(), rationale);
    }
}
