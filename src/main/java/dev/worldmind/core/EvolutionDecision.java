package dev.worldmind.core;

public record EvolutionDecision(EvolutionType type, double intensity, long decisionSeed, String rationale) {
    public EvolutionDecision {
        if (type == null) throw new IllegalArgumentException("type");
        intensity = Math.max(0.0, Math.min(1.0, intensity));
        rationale = rationale == null ? "" : rationale;
    }
}
