package dev.worldmind.core;

import java.util.SplittableRandom;

/** Pure, deterministic Worldmind decision kernel. No Minecraft classes belong here. */
public final class EvolutionEngine {
    private final double minimumAbsenceDays;

    public EvolutionEngine(double minimumAbsenceDays) {
        if (minimumAbsenceDays < 0) throw new IllegalArgumentException("minimumAbsenceDays");
        this.minimumAbsenceDays = minimumAbsenceDays;
    }

    public EvolutionDecision decide(PlaceSnapshot snapshot, long deterministicSeed) {
        if (snapshot.protectedPlace()) {
            return new EvolutionDecision(EvolutionType.STASIS, 0.0, deterministicSeed, "worldseal");
        }
        if (snapshot.absenceDays() < minimumAbsenceDays) {
            return new EvolutionDecision(EvolutionType.STASIS, 0.0, deterministicSeed, "absence-below-threshold");
        }
        if (snapshot.confidence() < 0.55) {
            return new EvolutionDecision(EvolutionType.STASIS, 0.0, deterministicSeed, "low-place-confidence");
        }

        double age = Math.min(1.0, (snapshot.absenceDays() - minimumAbsenceDays) / 40.0);
        double reclamation = 0.18 + 0.52 * snapshot.naturePressure() + 0.20 * age;
        double decay = 0.10 + 0.42 * snapshot.structuralFragility() + 0.30 * age + 0.12 * snapshot.threatPressure();
        double constructive = 0.08 + 0.58 * snapshot.settlementPressure() + 0.22 * age;
        double blended = 0.04 + 0.20 * age + 0.10 * Math.min(snapshot.naturePressure(), snapshot.settlementPressure());
        double stasis = Math.max(0.08, 0.45 - 0.28 * age);

        // Confidence scales change-bearing outcomes, preserving uncertainty restraint.
        double c = snapshot.confidence();
        reclamation *= c;
        decay *= c;
        constructive *= c;
        blended *= c;

        double total = stasis + reclamation + decay + constructive + blended;
        long mixedSeed = mix64(deterministicSeed ^ snapshot.placeId().getMostSignificantBits()
                ^ Long.rotateLeft(snapshot.placeId().getLeastSignificantBits(), 17));
        double roll = new SplittableRandom(mixedSeed).nextDouble(total);
        EvolutionType type;
        if ((roll -= stasis) < 0) type = EvolutionType.STASIS;
        else if ((roll -= reclamation) < 0) type = EvolutionType.RECLAMATION;
        else if ((roll -= decay) < 0) type = EvolutionType.DECAY;
        else if ((roll -= constructive) < 0) type = EvolutionType.CONSTRUCTIVE;
        else type = EvolutionType.BLENDED;

        double intensity = type == EvolutionType.STASIS ? 0.0 : Math.min(1.0, 0.20 + age * 0.65 + c * 0.15);
        return new EvolutionDecision(type, intensity, deterministicSeed, "contextual-weighted-causality");
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }
}
