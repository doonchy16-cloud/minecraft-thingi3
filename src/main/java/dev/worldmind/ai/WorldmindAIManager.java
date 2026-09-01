package dev.worldmind.ai;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side asynchronous AI coordinator. The manager never mutates Minecraft world state;
 * it only caches validated advisory records for deterministic simulation to optionally consume.
 */
public final class WorldmindAIManager implements AIAdviceSource {
    private static final long FAILURE_BACKOFF_TICKS = 2400L;
    private static final WorldmindAIManager GLOBAL = new WorldmindAIManager(
            WorldmindAIConfig.defaults(), new BuiltinIntelligenceProvider());

    private volatile WorldmindAIConfig config;
    private volatile WorldmindIntelligenceProvider provider;
    private final AIProposalValidator validator = new AIProposalValidator();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> backoffUntil = new ConcurrentHashMap<>();
    private final Set<String> inFlightPlaces = ConcurrentHashMap.newKeySet();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong clockTick = new AtomicLong();
    private volatile AIConnectionResult lastTest = AIConnectionResult.failed(0, "not_tested", "not tested");

    public WorldmindAIManager(WorldmindAIConfig config, WorldmindIntelligenceProvider provider) {
        this.config = (config == null ? WorldmindAIConfig.defaults() : config).validated();
        this.provider = provider == null ? new BuiltinIntelligenceProvider() : provider;
    }

    public static WorldmindAIManager global() { return GLOBAL; }

    public synchronized void reconfigure(WorldmindAIConfig next) {
        WorldmindAIConfig safe = (next == null ? WorldmindAIConfig.defaults() : next).validated();
        this.config = safe;
        this.provider = AIProviderFactory.create(safe);
        clearTransient();
        lastTest = AIConnectionResult.failed(0, "not_tested", "not tested");
    }

    public synchronized void shutdown() {
        config = WorldmindAIConfig.defaults();
        provider = new BuiltinIntelligenceProvider();
        clearTransient();
        lastTest = AIConnectionResult.failed(0, "not_tested", "not tested");
    }

    /** Clears per-server transient state while retaining the persisted provider configuration. */
    public synchronized void clearSession() {
        clearTransient();
        lastTest = AIConnectionResult.failed(0, "not_tested", "not tested");
    }

    public WorldmindAIConfig config() { return config; }

    /**
     * Schedules at most one non-blocking request for this place. Returns true only when a request was accepted.
     */
    public boolean consider(AIPlaceContext context, double minimumAbsenceDays, long nowTick) {
        clockTick.accumulateAndGet(Math.max(0L, nowTick), Math::max);
        WorldmindAIConfig cfg = config;
        if (context == null || !cfg.enabled() || !cfg.transformation() || cfg.provider() == AIProviderType.BUILTIN) return false;
        if (context.sealed() || context.placeConfidence() < AIProposalValidator.MIN_PLACE_CONFIDENCE
                || context.absenceDays() < Math.max(0.0, minimumAbsenceDays)) return false;
        String placeId = context.placeId();
        if (placeId.isBlank()) return false;

        CacheEntry existing = cache.get(placeId);
        if (existing != null && existing.expiresTick() > nowTick) return false;
        if (existing != null) cache.remove(placeId, existing);
        if (backoffUntil.getOrDefault(placeId, 0L) > nowTick) return false;
        if (inFlight.get() >= cfg.maxConcurrentRequests()) return false;
        if (!inFlightPlaces.add(placeId)) return false;
        if (inFlight.incrementAndGet() > cfg.maxConcurrentRequests()) {
            inFlight.decrementAndGet();
            inFlightPlaces.remove(placeId);
            return false;
        }

        long requestTick = nowTick;
        AIPlaceContext requestContext = cfg.structure() ? context : context.withoutStructureDetail();
        CompletableFuture<AITransformationProposal> future;
        try {
            future = provider.requestTransformation(requestContext);
        } catch (RuntimeException e) {
            finishFailure(placeId, requestTick);
            return false;
        }
        if (future == null) {
            finishFailure(placeId, requestTick);
            return false;
        }

        future.whenComplete((proposal, error) -> {
            try {
                if (error != null || proposal == null) {
                    backoffUntil.put(placeId, requestTick + FAILURE_BACKOFF_TICKS);
                    return;
                }
                Optional<AIValidatedAdvice> validated = validator.validate(context, proposal, minimumAbsenceDays);
                if (validated.isPresent()) {
                    long ttlTicks = Math.max(1L, Math.round(cfg.cacheDays() * 24000.0));
                    cache.put(placeId, new CacheEntry(validated.get(), requestTick + ttlTicks));
                    backoffUntil.remove(placeId);
                } else {
                    backoffUntil.put(placeId, requestTick + FAILURE_BACKOFF_TICKS);
                }
            } finally {
                inFlightPlaces.remove(placeId);
                inFlight.updateAndGet(v -> Math.max(0, v - 1));
            }
        });
        return true;
    }

    @Override
    public boolean planningPending(String placeId) {
        return placeId != null && inFlightPlaces.contains(placeId);
    }

    @Override
    public Optional<AIValidatedAdvice> adviceFor(String placeId) {
        if (placeId == null || placeId.isBlank()) return Optional.empty();
        CacheEntry entry = cache.get(placeId);
        if (entry == null) return Optional.empty();
        if (entry.expiresTick() <= clockTick.get()) {
            cache.remove(placeId, entry);
            return Optional.empty();
        }
        return Optional.of(entry.advice());
    }

    public CompletableFuture<AIConnectionResult> testConnection() {
        WorldmindIntelligenceProvider p = provider;
        CompletableFuture<AIConnectionResult> future;
        try { future = p.testConnection(); }
        catch (RuntimeException e) { future = CompletableFuture.completedFuture(AIConnectionResult.failed(0, "connection_failed", e.getClass().getSimpleName())); }
        if (future == null) future = CompletableFuture.completedFuture(AIConnectionResult.failed(0, "connection_failed", "no result"));
        return future.exceptionally(error -> AIConnectionResult.failed(0, "connection_failed",
                        error.getCause() == null ? error.getClass().getSimpleName() : error.getCause().getClass().getSimpleName()))
                .thenApply(result -> {
                    lastTest = result == null ? AIConnectionResult.failed(0, "connection_failed", "no result") : result;
                    return lastTest;
                });
    }

    public WorldmindAIStatus status() {
        WorldmindAIConfig cfg = config;
        return new WorldmindAIStatus(cfg.enabled(), cfg.provider(), cfg.endpoint(), cfg.model(),
                inFlight.get(), cache.size(), lastTest);
    }

    public void advanceClock(long tick) { clockTick.accumulateAndGet(Math.max(0L, tick), Math::max); }

    void setClockTickForTesting(long tick) { clockTick.set(Math.max(0L, tick)); }

    private void finishFailure(String placeId, long tick) {
        backoffUntil.put(placeId, tick + FAILURE_BACKOFF_TICKS);
        inFlightPlaces.remove(placeId);
        inFlight.updateAndGet(v -> Math.max(0, v - 1));
    }

    private void clearTransient() {
        cache.clear();
        backoffUntil.clear();
        inFlightPlaces.clear();
        inFlight.set(0);
        clockTick.set(0);
    }

    private record CacheEntry(AIValidatedAdvice advice, long expiresTick) {}
}
