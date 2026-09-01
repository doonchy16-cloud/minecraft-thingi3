package dev.worldmind.ai;

import java.net.URI;

public record AIHttpRequest(String method, URI uri, String body, int timeoutSeconds, int maxResponseBytes) {
    public AIHttpRequest {
        method = method == null ? "POST" : method.trim().toUpperCase(java.util.Locale.ROOT);
        if (uri == null) throw new IllegalArgumentException("uri");
        body = body == null ? "" : body;
        timeoutSeconds = Math.max(1, Math.min(60, timeoutSeconds));
        maxResponseBytes = Math.max(1024, Math.min(1_048_576, maxResponseBytes));
    }
}
