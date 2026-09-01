package dev.worldmind.ai;

public final class AIProviderFactory {
    private AIProviderFactory() {}
    public static WorldmindIntelligenceProvider create(WorldmindAIConfig config) { return create(config, new JavaAIHttpTransport()); }
    static WorldmindIntelligenceProvider create(WorldmindAIConfig config, AIHttpTransport transport) {
        WorldmindAIConfig safe = (config == null ? WorldmindAIConfig.defaults() : config).validated();
        return switch (safe.provider()) {
            case BUILTIN -> new BuiltinIntelligenceProvider();
            case OLLAMA -> new OllamaIntelligenceProvider(safe, transport);
            case FORGEY -> new ForgeyIntelligenceProvider(safe, transport);
            case COMPATIBLE -> new CompatibleIntelligenceProvider(safe, transport);
        };
    }
}
