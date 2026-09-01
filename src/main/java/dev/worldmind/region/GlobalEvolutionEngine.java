package dev.worldmind.region;

import java.util.SplittableRandom;

public final class GlobalEvolutionEngine {
    public RegionalDecision decide(RegionalSnapshot s, long seed) {
        EvolutionPressures p = s.pressures();
        if (s.elapsedDays() < 0.15) return new RegionalDecision(RegionalOutcome.STASIS, 0, false, "insufficient elapsed time");

        double age = Math.min(1.0, Math.log1p(s.elapsedDays()) / Math.log(101.0));
        double[] weights = new double[RegionalOutcome.values().length];
        weights[RegionalOutcome.STASIS.ordinal()] = Math.max(0.10, 0.8 - age * 0.55);
        weights[RegionalOutcome.VEGETATION_SPREAD.ordinal()] = 0.15 + p.vegetation() * (0.7 + age * 0.5) * (1.0 - p.disturbance() * 0.5);
        weights[RegionalOutcome.VEGETATION_THINNING.ordinal()] = p.disturbance() * 0.55;
        weights[RegionalOutcome.RECLAMATION.ordinal()] = p.weathering() * 0.4 + p.vegetation() * p.disturbance() * 0.75 + age * 0.20;
        weights[RegionalOutcome.FARM_SUCCESSION.ordinal()] = p.farmAbandonment() * (0.9 + age * 0.3);
        weights[RegionalOutcome.ROUTE_FORMATION.ordinal()] = p.routeWear() * 1.05;
        weights[RegionalOutcome.VILLAGE_GROWTH.ordinal()] = p.settlement() * (1.0 - p.scarcity()) * 0.75;
        weights[RegionalOutcome.VILLAGE_DECLINE.ordinal()] = p.settlement() * (p.scarcity() + p.threat()) * 0.55;
        weights[RegionalOutcome.FORTIFICATION.ordinal()] = p.settlement() * p.threat() * 0.65;
        weights[RegionalOutcome.PLACE_TRANSFORMATION.ordinal()] = p.historicalSignificance() * age * 0.45;
        weights[RegionalOutcome.HISTORIC_REOCCUPATION.ordinal()] = p.historicalSignificance() * p.settlement() * age * 0.35;
        weights[RegionalOutcome.ANOMALY_MANIFESTATION.ordinal()] = p.anomaly() * 0.0015;

        SplittableRandom random = new SplittableRandom(seed ^ s.key().stableId().hashCode());
        RegionalOutcome outcome = weighted(weights, random.nextDouble());
        double intensity = clamp(0.08 + age * 0.55 + random.nextDouble() * 0.15
                + Math.max(p.disturbance(), Math.max(p.settlement(), p.routeWear())) * 0.20);
        boolean physicalEligible = s.protectionCoverage() < 0.999 && outcome != RegionalOutcome.STASIS;
        return new RegionalDecision(outcome, outcome == RegionalOutcome.STASIS ? 0 : intensity, physicalEligible,
                rationale(outcome, p, age));
    }

    private static RegionalOutcome weighted(double[] weights, double roll) {
        double total = 0;
        for (double w : weights) total += Math.max(0, w);
        if (total <= 0) return RegionalOutcome.STASIS;
        double cursor = roll * total;
        RegionalOutcome[] values = RegionalOutcome.values();
        for (int i = 0; i < weights.length; i++) {
            cursor -= Math.max(0, weights[i]);
            if (cursor <= 0) return values[i];
        }
        return RegionalOutcome.STASIS;
    }

    private static String rationale(RegionalOutcome o, EvolutionPressures p, double age) {
        return switch (o) {
            case VEGETATION_SPREAD -> "vegetation pressure + elapsed age";
            case VEGETATION_THINNING -> "disturbance pressure";
            case RECLAMATION -> "weathering/recovery after disturbance";
            case FARM_SUCCESSION -> "abandoned farmland pressure";
            case ROUTE_FORMATION -> "repeated travel wear";
            case VILLAGE_GROWTH -> "settlement pressure with available resources";
            case VILLAGE_DECLINE -> "settlement under scarcity/threat";
            case FORTIFICATION -> "settlement threat response";
            case PLACE_TRANSFORMATION -> "historically significant place aged";
            case HISTORIC_REOCCUPATION -> "significant place + settlement opportunity";
            case ANOMALY_MANIFESTATION -> "extreme rare anomaly pressure";
            case STASIS -> "context favored no major physical change";
        };
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
