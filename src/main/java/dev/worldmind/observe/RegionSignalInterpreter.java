package dev.worldmind.observe;

import dev.worldmind.region.RegionState;

public final class RegionSignalInterpreter {
    private RegionSignalInterpreter() {}

    public static void apply(RegionState region, RegionalObservation o, long tick) {
        int d = o.denominator();
        double vegetation = clamp((o.vegetationBlocks() + o.logsLeaves() * 0.65) / (double)d);
        double forest = clamp(o.logsLeaves() / (double)d * 3.0);
        double farm = clamp(o.farmlandBlocks() / (double)d * 8.0);
        double village = clamp(o.villageMarkers() / (double)d * 12.0);
        double build = clamp(o.playerBuildBlocks() / (double)d * 4.0);
        double disturbance = clamp(o.disturbanceBlocks() / (double)d * 5.0);
        double travel = clamp(o.pathBlocks() / (double)d * 4.0);

        if (forest > 0.03) region.observeForest(forest);
        if (farm > 0.02) region.observeFarm(farm);
        if (village > 0.02) region.observeVillage(village);
        if (build > 0.02) region.observePlayerBuild(build);
        if (disturbance > 0.02) region.observeDisturbance(disturbance * 0.25);
        if (travel > 0.01) region.observeTravel(travel * 0.12);
        if (vegetation > 0.02 && disturbance < 0.4) region.observeHistoricalSignificance(vegetation * 0.01);
        region.markObserved(tick);
    }

    public static void applySpatial(RegionState region, int blockX, int blockZ, SpatialObservationSample sample, long tick) {
        var field = region.spatial();
        var key = region.key();
        int cx = field.cellXForBlock(key, blockX);
        int cz = field.cellZForBlock(key, blockZ);
        if (sample.vegetation() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.VEGETATION, 0.08);
        if (sample.logsLeaves() > 0) {
            field.add(cx, cz, dev.worldmind.region.SpatialSignal.FOREST, 0.14);
            field.add(cx, cz, dev.worldmind.region.SpatialSignal.VEGETATION, 0.06);
        }
        if (sample.farmland() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.FARM, 0.20);
        if (sample.village() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.SETTLEMENT, 0.25);
        if (sample.path() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.ROUTE, 0.10);
        if (sample.build() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.BUILD, 0.12);
        if (sample.disturbance() > 0) field.add(cx, cz, dev.worldmind.region.SpatialSignal.DISTURBANCE, 0.16);
        if (sample.seal() > 0) field.set(cx, cz, dev.worldmind.region.SpatialSignal.PROTECTION, 1.0);
        field.cell(cx, cz).markTouched(tick);
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
