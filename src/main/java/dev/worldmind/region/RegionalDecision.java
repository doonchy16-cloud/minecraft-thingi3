package dev.worldmind.region;

public record RegionalDecision(
        RegionalOutcome outcome,
        double intensity,
        boolean physicalEligible,
        String rationale) {}
