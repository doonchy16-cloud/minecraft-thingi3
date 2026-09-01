package dev.worldmind.ai;

import java.util.concurrent.CompletableFuture;

public final class BuiltinIntelligenceProvider implements WorldmindIntelligenceProvider {
    @Override public AIProviderType type() { return AIProviderType.BUILTIN; }
    @Override public String endpoint() { return ""; }
    @Override public String model() { return ""; }
    @Override public CompletableFuture<AIConnectionResult> testConnection() {
        return CompletableFuture.completedFuture(AIConnectionResult.failed(0, "builtin", "external AI disabled"));
    }
    @Override public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context) {
        return CompletableFuture.failedFuture(new IllegalStateException("external-ai-disabled"));
    }
}
