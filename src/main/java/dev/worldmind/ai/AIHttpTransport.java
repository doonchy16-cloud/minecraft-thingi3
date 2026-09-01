package dev.worldmind.ai;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AIHttpTransport {
    CompletableFuture<AIHttpResponse> send(AIHttpRequest request);
}
