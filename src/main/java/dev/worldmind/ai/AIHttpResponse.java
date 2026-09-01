package dev.worldmind.ai;

public record AIHttpResponse(int statusCode, String body, long latencyMillis) {
    public AIHttpResponse {
        body = body == null ? "" : body;
        latencyMillis = Math.max(0L, latencyMillis);
    }
    public boolean success() { return statusCode >= 200 && statusCode < 300; }
}
