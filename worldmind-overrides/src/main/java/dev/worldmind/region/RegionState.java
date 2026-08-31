package dev.worldmind.region;

public final class RegionState {
    private final RegionKey key;
    private final long knownSinceTick;
    private long lastEvaluationTick;
    private long lastMaterializationTick;
    private long lastObservedTick;
    private EvolutionPressures pressures;
    private double protectionCoverage;
    private double farmPresence;
    private double villagePresence;
    private double playerBuildPresence;
    private double forestPresence;
    private String civilizationId;

    private RegionState(RegionKey key, long knownSinceTick) {
        this.key = key;
        this.knownSinceTick = knownSinceTick;
        this.lastEvaluationTick = knownSinceTick;
        this.lastMaterializationTick = knownSinceTick;
        this.lastObservedTick = knownSinceTick;
        this.pressures = EvolutionPressures.baseline();
    }

    public static RegionState fresh(RegionKey key, long tick) { return new RegionState(key, tick); }
    public RegionKey key() { return key; }
    public long knownSinceTick() { return knownSinceTick; }
    public long lastEvaluationTick() { return lastEvaluationTick; }
    public long lastMaterializationTick() { return lastMaterializationTick; }
    public long lastObservedTick() { return lastObservedTick; }
    public EvolutionPressures pressures() { return pressures; }
    public double protectionCoverage() { return protectionCoverage; }
    public double farmPresence() { return farmPresence; }
    public double villagePresence() { return villagePresence; }
    public double playerBuildPresence() { return playerBuildPresence; }
    public double forestPresence() { return forestPresence; }
    public String civilizationId() { return civilizationId; }

    public void observeDisturbance(double amount) { pressures = pressures.withDisturbance(amount); }
    public void observeTravel(double amount) { pressures = pressures.withTravel(amount); }
    public void observeFarmActivity(double amount) { pressures = pressures.withFarmActivity(amount); farmPresence = Math.max(farmPresence, clamp(amount)); }
    public void observeFarm(double amount) { farmPresence = Math.max(farmPresence, clamp(amount)); pressures = pressures.withFarmActivity(amount * 0.5); }
    public void observeVillage(double amount) { villagePresence = Math.max(villagePresence, clamp(amount)); pressures = pressures.withSettlement(amount); }
    public void observePlayerBuild(double amount) { playerBuildPresence = Math.max(playerBuildPresence, clamp(amount)); pressures = pressures.withHistoricalSignificance(amount * 0.25); }
    public void observeForest(double amount) { forestPresence = Math.max(forestPresence, clamp(amount)); }
    public void setCivilizationId(String id) { civilizationId = id; }
    public void observeFarmAbandonment(double amount) { pressures = pressures.withFarmAbandonment(amount); }
    public void observeSettlement(double amount) { pressures = pressures.withSettlement(amount); }
    public void observeHistoricalSignificance(double amount) { pressures = pressures.withHistoricalSignificance(amount); }
    public void markObserved(long tick) { lastObservedTick = Math.max(lastObservedTick, tick); }
    public void setProtectionCoverage(double coverage) { protectionCoverage = clamp(coverage); }

    public void ageTo(long tick) {
        if (tick <= lastEvaluationTick) return;
        double days = (tick - lastEvaluationTick) / 24000.0;
        pressures = pressures.age(days);
        double unobservedDays = Math.max(0, tick - lastObservedTick) / 24000.0;
        if (farmPresence > 0 && unobservedDays >= 2.0) {
            pressures = pressures.withFarmAbandonment(Math.min(0.35, farmPresence * Math.log1p(unobservedDays) * 0.08));
        }
        if (forestPresence > 0 && pressures.disturbance() > 0) {
            pressures = pressures.withHistoricalSignificance(Math.min(0.08, forestPresence * days * 0.002));
        }
        lastEvaluationTick = tick;
    }

    public void markMaterialized(long tick) { lastMaterializationTick = Math.max(lastMaterializationTick, tick); }

    public RegionalSnapshot snapshot(long tick) {
        double evaluationDays = Math.max(0, tick - lastEvaluationTick) / 24000.0;
        double physicalAgeDays = Math.max(0, tick - lastMaterializationTick) / 24000.0;
        return new RegionalSnapshot(key, physicalAgeDays, protectionCoverage, pressures.age(evaluationDays), knownSinceTick,
                lastEvaluationTick, lastMaterializationTick);
    }

    public double priority(long nowTick) {
        double daysSinceEval = Math.max(0, nowTick - lastEvaluationTick) / 24000.0;
        EvolutionPressures p = pressures;
        return Math.log1p(daysSinceEval) + p.disturbance() * 1.4 + p.settlement() * 1.1 + p.routeWear() * 0.7
                + p.farmAbandonment() * 0.8 + p.historicalSignificance() * 1.2 + p.anomaly() * 0.2;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
