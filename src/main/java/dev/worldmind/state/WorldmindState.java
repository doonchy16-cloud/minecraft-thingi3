package dev.worldmind.state;

import dev.worldmind.protect.ProtectionRecord;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionState;
import dev.worldmind.civ.CivilizationState;
import dev.worldmind.history.HistoryGraph;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorldmindState {
    private long worldTicks;
    private long lastRealTimeMillis = System.currentTimeMillis();
    private Map<String, PlaceRecord> places = new LinkedHashMap<>();
    private Map<String, TransformationPlan> plans = new LinkedHashMap<>();
    private List<ProtectionRecord> protections = new ArrayList<>();
    private Map<String, Integer> protectionCountByRegion = new LinkedHashMap<>();
    private Map<String, RegionState> regions = new LinkedHashMap<>();
    private Map<String, RegionTransformationPlan> regionPlans = new LinkedHashMap<>();
    private Map<String, String> pendingRegionPlanByRegion = new LinkedHashMap<>();
    private Map<String, CivilizationState> civilizations = new LinkedHashMap<>();
    private HistoryGraph history = new HistoryGraph();

    public long worldTicks() { return worldTicks; }
    public void setWorldTicks(long worldTicks) { this.worldTicks = Math.max(0L, worldTicks); }
    public long incrementWorldTicks() { return ++worldTicks; }
    public long lastRealTimeMillis() { return lastRealTimeMillis; }
    public void setLastRealTimeMillis(long value) { lastRealTimeMillis = Math.max(0L, value); }

    public Map<String, PlaceRecord> places() { return Map.copyOf(places); }
    public Map<String, TransformationPlan> plans() { return Map.copyOf(plans); }
    public List<ProtectionRecord> protections() { return List.copyOf(protections); }
    public Map<String, RegionState> regions() { return Map.copyOf(regions); }
    public Map<String, RegionTransformationPlan> regionPlans() { return Map.copyOf(regionPlans); }
    public Map<String, CivilizationState> civilizations() { return Map.copyOf(civilizations); }
    public HistoryGraph history() { if (history == null) history = new HistoryGraph(); return history; }

    public void upsertPlace(PlaceRecord place) { places.put(place.id(), place); }
    public Optional<PlaceRecord> place(String id) { return Optional.ofNullable(places.get(id)); }
    public void addPlan(TransformationPlan plan) { plans.putIfAbsent(plan.id(), plan); }
    public Optional<TransformationPlan> plan(String id) { return Optional.ofNullable(plans.get(id)); }
    public void markCommitted(String id) { Optional.ofNullable(plans.get(id)).ifPresent(TransformationPlan::markCommitted); }
    public void markDeferred(String id) { Optional.ofNullable(plans.get(id)).ifPresent(TransformationPlan::markDeferred); }
    public void addProtection(ProtectionRecord record) {
        protections.add(record);
        ensureProtectionIndex();
        RegionKey key = RegionKey.fromBlock(record.dimension(), record.x(), record.z());
        protectionCountByRegion.merge(key.stableId(), 1, Integer::sum);
    }
    public void replaceProtections(List<ProtectionRecord> records) {
        protections = new ArrayList<>(records == null ? List.of() : records);
        rebuildProtectionIndex();
    }
    public int protectionCount(RegionKey key) {
        ensureProtectionIndex();
        return protectionCountByRegion.getOrDefault(key.stableId(), 0);
    }
    private void ensureProtectionIndex() {
        if (protectionCountByRegion == null) rebuildProtectionIndex();
    }
    private void rebuildProtectionIndex() {
        protectionCountByRegion = new LinkedHashMap<>();
        if (protections == null) protections = new ArrayList<>();
        for (ProtectionRecord record : protections) {
            RegionKey key = RegionKey.fromBlock(record.dimension(), record.x(), record.z());
            protectionCountByRegion.merge(key.stableId(), 1, Integer::sum);
        }
    }
    public void upsertRegion(RegionState region) { regions.put(region.key().stableId(), region); }
    public Optional<RegionState> region(RegionKey key) { return Optional.ofNullable(regions.get(key.stableId())); }
    public RegionState getOrCreateRegion(RegionKey key) { return regions.computeIfAbsent(key.stableId(), k -> RegionState.fresh(key, worldTicks)); }
    public void addRegionPlan(RegionTransformationPlan plan) {
        regionPlans.putIfAbsent(plan.id(), plan);
        ensurePendingIndex();
        if (plan.status() == PlanStatus.PENDING) pendingRegionPlanByRegion.putIfAbsent(plan.region().stableId(), plan.id());
    }
    public boolean hasPendingPlanForRegion(RegionKey key) { return pendingRegionPlan(key).isPresent(); }
    public Optional<RegionTransformationPlan> pendingRegionPlan(RegionKey key) {
        ensurePendingIndex();
        String id = pendingRegionPlanByRegion.get(key.stableId());
        RegionTransformationPlan plan = id == null ? null : regionPlans.get(id);
        if (plan != null && plan.status() == PlanStatus.PENDING) return Optional.of(plan);
        pendingRegionPlanByRegion.remove(key.stableId());
        for (RegionTransformationPlan candidate : regionPlans.values()) {
            if (candidate.region().equals(key) && candidate.status() == PlanStatus.PENDING) {
                pendingRegionPlanByRegion.put(key.stableId(), candidate.id());
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
    public void markRegionPlanCommitted(String id) {
        RegionTransformationPlan plan = regionPlans.get(id);
        if (plan == null) return;
        plan.markCommitted();
        ensurePendingIndex();
        pendingRegionPlanByRegion.remove(plan.region().stableId(), id);
    }
    public void markRegionPlanDeferred(String id) {
        RegionTransformationPlan plan = regionPlans.get(id);
        if (plan == null) return;
        plan.markDeferred();
        ensurePendingIndex();
        pendingRegionPlanByRegion.remove(plan.region().stableId(), id);
    }
    private void ensurePendingIndex() {
        if (pendingRegionPlanByRegion == null) pendingRegionPlanByRegion = new LinkedHashMap<>();
    }
    public void upsertCivilization(CivilizationState civilization) { civilizations.put(civilization.id(), civilization); }

    public Optional<PlaceRecord> nearestOwnedPlace(String dimension, int x, int y, int z, String ownerId, int maxDistance) {
        long max2 = (long) maxDistance * maxDistance;
        return places.values().stream()
                .filter(p -> p.dimension().equals(dimension))
                .filter(p -> p.ownerIds().contains(ownerId))
                .filter(p -> p.distanceSquared(x, y, z) <= max2)
                .min((a,b) -> Long.compare(a.distanceSquared(x,y,z), b.distanceSquared(x,y,z)));
    }

    public boolean hasPendingPlanForPlace(String placeId) {
        return plans.values().stream().anyMatch(p -> p.placeId().equals(placeId) && p.status() == PlanStatus.PENDING);
    }
}
