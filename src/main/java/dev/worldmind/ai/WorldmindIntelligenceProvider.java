package dev.worldmind.ai;

import java.util.concurrent.CompletableFuture;

public interface WorldmindIntelligenceProvider {
    AIProviderType type();
    String endpoint();
    String model();
    CompletableFuture<AIConnectionResult> testConnection();
    CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context);
}
