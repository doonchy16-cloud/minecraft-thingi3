package dev.worldmind.ai;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

abstract class AbstractHttpIntelligenceProvider implements WorldmindIntelligenceProvider {
    static final int MAX_RESPONSE_BYTES = 65_536;
    static final String SYSTEM_PROMPT = "You are Worldmind's advisory intelligence. Return ONLY one JSON object with keys placeId, recommendation, confidence, intensityAdjustment, reason, styleHint. recommendation must be STASIS, RECLAMATION, DECAY, CONSTRUCTIVE, or BLENDED. intensityAdjustment must be between -0.15 and 0.15. Never return Minecraft commands, block coordinates, code, or action lists.";

    protected final WorldmindAIConfig config;
    protected final AIHttpTransport transport;

    AbstractHttpIntelligenceProvider(WorldmindAIConfig config, AIHttpTransport transport) {
        this.config = (config == null ? WorldmindAIConfig.defaults() : config).validated();
        this.transport = transport == null ? new JavaAIHttpTransport() : transport;
    }

    @Override public AIProviderType type() { return config.provider(); }
    @Override public String endpoint() { return config.endpoint(); }
    @Override public String model() { return config.model(); }

    protected CompletableFuture<AIHttpResponse> send(String method, URI uri, String body) {
        return transport.send(new AIHttpRequest(method, uri, body, config.timeoutSeconds(), MAX_RESPONSE_BYTES))
                .thenApply(response -> {
                    if (!response.success()) throw new CompletionException(new IllegalStateException("ai-http-" + response.statusCode()));
                    if (response.body().getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
                        throw new CompletionException(new IllegalStateException("ai-response-too-large"));
                    }
                    return response;
                });
    }

    protected static URI uri(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("ai-endpoint-missing");
        return URI.create(value);
    }

    protected static AIConnectionResult connectionFrom(AIHttpResponse response, String detail) {
        return AIConnectionResult.ok(response.latencyMillis(), detail);
    }

    protected static CompletableFuture<AIConnectionResult> failedConnection(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String category = cause instanceof java.net.http.HttpTimeoutException ? "timeout" : "connection_failed";
        return CompletableFuture.completedFuture(AIConnectionResult.failed(0, category, cause.getClass().getSimpleName()));
    }
}
