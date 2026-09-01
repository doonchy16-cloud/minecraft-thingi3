package dev.worldmind.ai;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class OllamaIntelligenceProvider extends AbstractHttpIntelligenceProvider {
    public OllamaIntelligenceProvider(WorldmindAIConfig config, AIHttpTransport transport) { super(config, transport); }

    @Override public CompletableFuture<AIConnectionResult> testConnection() {
        if (config.model().isBlank()) return CompletableFuture.completedFuture(AIConnectionResult.failed(0, "model_missing", "set /worldmind ai model"));
        String body = chatBody("Return only {\"ok\":true}.");
        return send("POST", chatUri(), body).thenApply(r -> connectionFrom(r, "Ollama responded"))
                .exceptionallyCompose(AbstractHttpIntelligenceProvider::failedConnection);
    }

    @Override public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context) {
        if (config.model().isBlank()) return CompletableFuture.failedFuture(new IllegalStateException("ai-model-missing"));
        String body = chatBody("Evaluate this Worldmind place context and return the strict proposal JSON: " + WorldmindAIJson.contextJson(context));
        return send("POST", chatUri(), body).thenApply(r -> {
            String content = WorldmindAIJson.stringValue(r.body(), "content");
            if (content == null) throw new IllegalArgumentException("ollama-content-missing");
            return WorldmindAIJson.parseProposal(content);
        });
    }

    private URI chatUri() { return uri(config.endpoint() + "/api/chat"); }
    private String chatBody(String user) {
        return "{\"model\":" + WorldmindAIJson.quote(config.model()) +
                ",\"stream\":false,\"format\":\"json\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":" + WorldmindAIJson.quote(SYSTEM_PROMPT) + "}," +
                "{\"role\":\"user\",\"content\":" + WorldmindAIJson.quote(user) + "}]}";
    }
}
