package dev.worldmind.ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class JavaAIHttpTransport implements AIHttpTransport {
    private final HttpClient client;

    public JavaAIHttpTransport() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    JavaAIHttpTransport(HttpClient client) { this.client = client; }

    @Override
    public CompletableFuture<AIHttpResponse> send(AIHttpRequest request) {
        long start = System.nanoTime();
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(Duration.ofSeconds(request.timeoutSeconds()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Worldmind/1.2");
        if ("GET".equals(request.method())) builder.GET();
        else builder.method(request.method(), HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    try (InputStream in = response.body()) {
                        byte[] bytes = in.readNBytes(request.maxResponseBytes() + 1);
                        if (bytes.length > request.maxResponseBytes()) {
                            throw new IOException("ai-response-too-large");
                        }
                        long latency = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
                        return new AIHttpResponse(response.statusCode(), new String(bytes, StandardCharsets.UTF_8), latency);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
    }
}
