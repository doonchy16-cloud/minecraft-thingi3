package dev.worldmind.ai;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class CompatibleIntelligenceProvider extends AbstractHttpIntelligenceProvider {
    public CompatibleIntelligenceProvider(WorldmindAIConfig config, AIHttpTransport transport) { super(config, transport); }

    @Override public CompletableFuture<AIConnectionResult> testConnection() {
        if (config.model().isBlank()) return CompletableFuture.completedFuture(AIConnectionResult.failed(0, "model_missing", "set /worldmind ai model"));
        return send("POST", chatUri(), chatBody("Return only {\"ok\":true}."))
                .thenApply(r -> connectionFrom(r, "compatible endpoint responded"))
                .exceptionallyCompose(AbstractHttpIntelligenceProvider::failedConnection);
    }

    @Override public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context) {
        if (config.model().isBlank()) return CompletableFuture.failedFuture(new IllegalStateException("ai-model-missing"));
        return send("POST", chatUri(), chatBody("Evaluate this Worldmind place context and return strict proposal JSON: " + WorldmindAIJson.contextJson(context)))
                .thenApply(r -> {
                    String content = WorldmindAIJson.stringValue(r.body(), "content");
                    if (content == null) throw new IllegalArgumentException("compatible-content-missing");
                    return WorldmindAIJson.parseProposal(content);
                });
    }

    private URI chatUri() {
        String endpoint = config.endpoint();
        if (endpoint.endsWith("/v1/chat/completions")) return uri(endpoint);
        if (endpoint.endsWith("/v1")) return uri(endpoint + "/chat/completions");
        return uri(endpoint + "/v1/chat/completions");
    }

    private String chatBody(String user) {
        return "{\"model\":" + WorldmindAIJson.quote(config.model()) + ",\"messages\":[" +
                "{\"role\":\"system\",\"content\":" + WorldmindAIJson.quote(SYSTEM_PROMPT) + "}," +
                "{\"role\":\"user\",\"content\":" + WorldmindAIJson.quote(user) + "}],\"temperature\":0.2}";
    }
}
