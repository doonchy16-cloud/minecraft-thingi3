package dev.worldmind.command;

import dev.worldmind.ai.AIConnectionResult;
import dev.worldmind.ai.AIFeature;
import dev.worldmind.ai.AIProviderType;
import dev.worldmind.ai.WorldmindAIConfig;
import dev.worldmind.ai.WorldmindAIStatus;

/** Command-independent AI configuration operations. Persistence is performed by the command adapter. */
public final class WorldmindAIAdminService {
    public WorldmindAIConfig enable(WorldmindAIConfig current, boolean enabled) {
        return safe(current).withEnabled(enabled);
    }

    public WorldmindAIConfig provider(WorldmindAIConfig current, AIProviderType provider) {
        return safe(current).withProvider(provider);
    }

    public WorldmindAIConfig endpoint(WorldmindAIConfig current, String endpoint) {
        return safe(current).withEndpoint(endpoint);
    }

    public WorldmindAIConfig model(WorldmindAIConfig current, String model) {
        return safe(current).withModel(model);
    }

    public WorldmindAIConfig feature(WorldmindAIConfig current, AIFeature feature, boolean enabled) {
        return safe(current).withFeature(feature, enabled);
    }

    public WorldmindAIConfig ollama(WorldmindAIConfig current) {
        WorldmindAIConfig base = safe(current);
        return new WorldmindAIConfig(base.enabled(), AIProviderType.OLLAMA, "http://localhost:11434", base.model(),
                base.timeoutSeconds(), base.maxConcurrentRequests(), base.cacheDays(), base.structure(),
                base.transformation(), base.civilization(), base.history(), base.naming()).validated();
    }

    public WorldmindAIConfig forgey(WorldmindAIConfig current, String endpoint) {
        WorldmindAIConfig base = safe(current);
        return new WorldmindAIConfig(base.enabled(), AIProviderType.FORGEY, endpoint, base.model(),
                base.timeoutSeconds(), base.maxConcurrentRequests(), base.cacheDays(), base.structure(),
                base.transformation(), base.civilization(), base.history(), base.naming()).validated();
    }

    public String status(WorldmindAIConfig config, WorldmindAIStatus status) {
        WorldmindAIConfig cfg = safe(config);
        WorldmindAIStatus s = status == null
                ? new WorldmindAIStatus(cfg.enabled(), cfg.provider(), cfg.endpoint(), cfg.model(), 0, 0,
                        AIConnectionResult.failed(0, "not_tested", "not tested"))
                : status;
        AIConnectionResult test = s.lastTest();
        return "Worldmind AI: " + (cfg.enabled() ? "ENABLED" : "DISABLED")
                + " provider=" + cfg.provider()
                + " endpoint=" + (cfg.endpoint().isEmpty() ? "<unset>" : cfg.endpoint())
                + " model=" + (cfg.model().isEmpty() ? "<unset>" : cfg.model())
                + " uses=[structure=" + cfg.structure()
                + ",transformation=" + cfg.transformation()
                + ",civilization=" + cfg.civilization()
                + ",history=" + cfg.history()
                + ",naming=" + cfg.naming() + "]"
                + " inFlight=" + s.inFlight()
                + " cached=" + s.cachedAdvice()
                + " lastTest=" + (test == null ? "not_tested" : test.category())
                + (test != null && test.latencyMillis() > 0 ? "@" + test.latencyMillis() + "ms" : "");
    }

    private static WorldmindAIConfig safe(WorldmindAIConfig c) {
        return (c == null ? WorldmindAIConfig.defaults() : c).validated();
    }
}
