package dev.worldmind.region;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SpatialProcessPlanner {
    public SpatialProcessPlan plan(RegionState region, RegionalDecision decision, long seed, int maxMutations, long nowTick) {
        SpatialProcess process = processFor(decision.outcome(), region);
        if (process == SpatialProcess.NONE || !decision.physicalEligible()) return new SpatialProcessPlan(SpatialProcess.NONE, new int[0], 0);
        double ageDays = region.snapshot(nowTick).elapsedDays();
        int targetMutations = magnitude(ageDays, decision.intensity(), maxMutations);
        int desiredCells = Math.max(1, Math.min(12, 1 + targetMutations / 48));
        int[] cells = switch (process) {
            case ROUTE_CORRIDOR -> routeCells(region.spatial(), desiredCells);
            case FOREST_FRONTIER -> cluster(region.spatial(), SpatialSignal.FOREST, desiredCells, seed);
            case FARM_SUCCESSION_PATCH -> cluster(region.spatial(), SpatialSignal.FARM, desiredCells, seed);
            case SETTLEMENT_GROWTH_EDGE, SETTLEMENT_DECLINE_PATCH, FORTIFICATION_EDGE ->
                    cluster(region.spatial(), SpatialSignal.SETTLEMENT, desiredCells, seed);
            case PLACE_DOMAIN, HISTORIC_REOCCUPATION_SITE -> cluster(region.spatial(), SpatialSignal.BUILD, desiredCells, seed);
            case RECLAMATION_PATCH -> cluster(region.spatial(), bestReclamationSignal(region.spatial()), desiredCells, seed);
            case VEGETATION_PATCH -> cluster(region.spatial(), SpatialSignal.VEGETATION, desiredCells, seed);
            case ANOMALY_SITE -> new int[] { region.spatial().strongest(SpatialSignal.DISTURBANCE) };
            case NONE -> new int[0];
        };
        if (cells.length == 0) {
            int fallback = firstUnprotected(region.spatial(), seed);
            if (fallback < 0) return new SpatialProcessPlan(SpatialProcess.NONE, new int[0], 0);
            cells = new int[] { fallback };
        }
        return new SpatialProcessPlan(process, cells, targetMutations);
    }

    public int magnitude(double ageDays, double intensity, int maxMutations) {
        int max = Math.max(1, maxMutations);
        double age = Math.max(0.0, ageDays);
        double ageScale = Math.min(1.35, Math.log1p(age) / Math.log(31.0));
        double factor = 0.28 + Math.max(0.0, ageScale) * 0.72;
        int target = (int)Math.round(max * clamp(intensity) * factor);
        return Math.max(1, Math.min(max, target));
    }

    private static SpatialProcess processFor(RegionalOutcome outcome, RegionState region) {
        return switch (outcome) {
            case STASIS -> SpatialProcess.NONE;
            case VEGETATION_SPREAD -> strongest(region.spatial(), SpatialSignal.FOREST) > 0.08
                    ? SpatialProcess.FOREST_FRONTIER : SpatialProcess.VEGETATION_PATCH;
            case VEGETATION_THINNING -> SpatialProcess.VEGETATION_PATCH;
            case RECLAMATION -> SpatialProcess.RECLAMATION_PATCH;
            case FARM_SUCCESSION -> SpatialProcess.FARM_SUCCESSION_PATCH;
            case ROUTE_FORMATION -> SpatialProcess.ROUTE_CORRIDOR;
            case VILLAGE_GROWTH -> SpatialProcess.SETTLEMENT_GROWTH_EDGE;
            case VILLAGE_DECLINE -> SpatialProcess.SETTLEMENT_DECLINE_PATCH;
            case FORTIFICATION -> SpatialProcess.FORTIFICATION_EDGE;
            case PLACE_TRANSFORMATION -> SpatialProcess.PLACE_DOMAIN;
            case HISTORIC_REOCCUPATION -> SpatialProcess.HISTORIC_REOCCUPATION_SITE;
            case ANOMALY_MANIFESTATION -> SpatialProcess.ANOMALY_SITE;
        };
    }

    private static int[] routeCells(SpatialField field, int limit) {
        List<Integer> hot = new ArrayList<>(field.strongestCells(SpatialSignal.ROUTE, Math.max(2, limit * 2), 0.04));
        hot.removeIf(i -> protectedCell(field, i));
        if (hot.isEmpty()) return new int[0];
        if (hot.size() > Math.max(2, limit)) hot = new ArrayList<>(hot.subList(0, Math.max(2, limit)));
        hot.sort(Comparator.comparingInt(field::x).thenComparingInt(field::z));
        return hot.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] cluster(SpatialField field, SpatialSignal signal, int limit, long seed) {
        int anchor = strongestUnprotected(field, signal);
        if (anchor < 0) return new int[0];
        Set<Integer> seen = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        List<Integer> out = new ArrayList<>();
        queue.add(anchor);
        seen.add(anchor);
        while (!queue.isEmpty() && out.size() < limit) {
            int current = queue.removeFirst();
            out.add(current);
            int x = field.x(current), z = field.z(current);
            List<Integer> next = new ArrayList<>(field.cardinalNeighbors(x,z));
            next.sort(Comparator.<Integer>comparingDouble(i -> field.cell(field.x(i), field.z(i)).signal(signal)).reversed()
                    .thenComparingLong(i -> mix(seed ^ i)));
            for (int n : next) if (!protectedCell(field, n) && seen.add(n)) queue.addLast(n);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean protectedCell(SpatialField field, int index) {
        return field.cell(field.x(index), field.z(index)).signal(SpatialSignal.PROTECTION) >= 0.5;
    }

    private static int strongestUnprotected(SpatialField field, SpatialSignal signal) {
        int best = -1; double score = -1;
        for (int z=0; z<SpatialField.GRID_SIZE; z++) for (int x=0; x<SpatialField.GRID_SIZE; x++) {
            int i = field.index(x,z);
            if (protectedCell(field,i)) continue;
            double value = field.cell(x,z).signal(signal);
            if (value > score) { score = value; best = i; }
        }
        return best;
    }

    private static int firstUnprotected(SpatialField field, long seed) {
        int total = SpatialField.GRID_SIZE * SpatialField.GRID_SIZE;
        int start = Math.floorMod((int)seed, total);
        for (int n=0; n<total; n++) {
            int i = (start + n) % total;
            if (!protectedCell(field,i)) return i;
        }
        return -1;
    }

    private static SpatialSignal bestReclamationSignal(SpatialField field) {
        double build = strongest(field, SpatialSignal.BUILD);
        double disturbance = strongest(field, SpatialSignal.DISTURBANCE);
        double reclamation = strongest(field, SpatialSignal.RECLAMATION);
        if (build >= disturbance && build >= reclamation) return SpatialSignal.BUILD;
        return disturbance >= reclamation ? SpatialSignal.DISTURBANCE : SpatialSignal.RECLAMATION;
    }

    private static double strongest(SpatialField field, SpatialSignal signal) {
        int i = field.strongest(signal);
        return field.cell(field.x(i), field.z(i)).signal(signal);
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }
}
