package dev.worldmind.region;

public record EvolutionPressures(
        double vegetation,
        double disturbance,
        double weathering,
        double settlement,
        double routeWear,
        double farmAbandonment,
        double scarcity,
        double threat,
        double anomaly,
        double historicalSignificance) {

    public EvolutionPressures {
        vegetation = clamp(vegetation);
        disturbance = clamp(disturbance);
        weathering = clamp(weathering);
        settlement = clamp(settlement);
        routeWear = clamp(routeWear);
        farmAbandonment = clamp(farmAbandonment);
        scarcity = clamp(scarcity);
        threat = clamp(threat);
        anomaly = clamp(anomaly);
        historicalSignificance = clamp(historicalSignificance);
    }

    public static EvolutionPressures baseline() {
        return new EvolutionPressures(0.35, 0, 0.05, 0, 0, 0, 0.05, 0.05, 0, 0);
    }

    public EvolutionPressures age(double days) {
        double slow = Math.min(1.0, Math.log1p(Math.max(0, days)) / Math.log(101.0));
        return new EvolutionPressures(
                vegetation + 0.18 * slow * (1.0 - disturbance),
                disturbance * (1.0 - 0.06 * slow),
                weathering + 0.22 * slow,
                settlement,
                routeWear * (1.0 - 0.025 * slow),
                farmAbandonment + 0.12 * slow,
                scarcity,
                threat,
                anomaly + 0.002 * slow,
                historicalSignificance);
    }

    public EvolutionPressures withDisturbance(double amount) {
        return new EvolutionPressures(vegetation * (1.0 - 0.30 * clamp(amount)), disturbance + amount,
                weathering, settlement, routeWear, farmAbandonment, scarcity, threat, anomaly, historicalSignificance);
    }

    public EvolutionPressures withTravel(double amount) {
        return new EvolutionPressures(vegetation, disturbance, weathering, settlement,
                routeWear + 0.35 * amount, farmAbandonment, scarcity, threat, anomaly, historicalSignificance);
    }

    public EvolutionPressures withFarmActivity(double amount) {
        return new EvolutionPressures(vegetation, disturbance, weathering, settlement,
                routeWear, farmAbandonment - 0.5 * amount, scarcity, threat, anomaly, historicalSignificance);
    }

    public EvolutionPressures withFarmAbandonment(double amount) {
        return new EvolutionPressures(vegetation, disturbance, weathering, settlement,
                routeWear, farmAbandonment + amount, scarcity, threat, anomaly, historicalSignificance);
    }

    public EvolutionPressures withSettlement(double amount) {
        return new EvolutionPressures(vegetation, disturbance, weathering, settlement + amount,
                routeWear, farmAbandonment, scarcity, threat, anomaly, historicalSignificance + 0.1 * amount);
    }

    public EvolutionPressures withHistoricalSignificance(double amount) {
        return new EvolutionPressures(vegetation, disturbance, weathering, settlement, routeWear,
                farmAbandonment, scarcity, threat, anomaly, historicalSignificance + amount);
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
