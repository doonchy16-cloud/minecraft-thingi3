package dev.worldmind.ai;

import java.net.URI;

public record WorldmindAIConfig(
        boolean enabled,
        AIProviderType provider,
        String endpoint,
        String model,
        int timeoutSeconds,
        int maxConcurrentRequests,
        double cacheDays,
        boolean structure,
        boolean transformation,
        boolean civilization,
        boolean history,
        boolean naming) {

    public static WorldmindAIConfig defaults() {
        return new WorldmindAIConfig(false, AIProviderType.BUILTIN, "", "", 8, 1, 5.0,
                true, true, false, false, false);
    }

    public WorldmindAIConfig validated() {
        AIProviderType safeProvider = provider == null ? AIProviderType.BUILTIN : provider;
        String safeEndpoint = normalizeEndpoint(endpoint);
        String safeModel = model == null ? "" : model.trim();
        boolean safeEnabled = enabled;
        if (safeProvider == AIProviderType.BUILTIN) safeEnabled = false;
        if (safeProvider != AIProviderType.BUILTIN && safeEndpoint.isEmpty()) safeEnabled = false;
        return new WorldmindAIConfig(
                safeEnabled,
                safeProvider,
                safeEndpoint,
                safeModel,
                clamp(timeoutSeconds, 1, 60),
                clamp(maxConcurrentRequests, 1, 4),
                clamp(cacheDays, 0.25, 30.0),
                structure,
                transformation,
                civilization,
                history,
                naming);
    }

    public WorldmindAIConfig withEnabled(boolean value) {
        return new WorldmindAIConfig(value, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                structure, transformation, civilization, history, naming).validated();
    }

    public WorldmindAIConfig withProvider(AIProviderType value) {
        return new WorldmindAIConfig(enabled, value, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                structure, transformation, civilization, history, naming).validated();
    }

    public WorldmindAIConfig withEndpoint(String value) {
        return new WorldmindAIConfig(enabled, provider, value, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                structure, transformation, civilization, history, naming).validated();
    }

    public WorldmindAIConfig withModel(String value) {
        return new WorldmindAIConfig(enabled, provider, endpoint, value, timeoutSeconds, maxConcurrentRequests, cacheDays,
                structure, transformation, civilization, history, naming).validated();
    }

    public WorldmindAIConfig withFeature(AIFeature feature, boolean value) {
        if (feature == null) return this;
        return switch (feature) {
            case STRUCTURE -> new WorldmindAIConfig(enabled, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                    value, transformation, civilization, history, naming).validated();
            case TRANSFORMATION -> new WorldmindAIConfig(enabled, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                    structure, value, civilization, history, naming).validated();
            case CIVILIZATION -> new WorldmindAIConfig(enabled, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                    structure, transformation, value, history, naming).validated();
            case HISTORY -> new WorldmindAIConfig(enabled, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                    structure, transformation, civilization, value, naming).validated();
            case NAMING -> new WorldmindAIConfig(enabled, provider, endpoint, model, timeoutSeconds, maxConcurrentRequests, cacheDays,
                    structure, transformation, civilization, history, value).validated();
        };
    }

    public boolean featureEnabled(AIFeature feature) {
        return switch (feature) {
            case STRUCTURE -> structure;
            case TRANSFORMATION -> transformation;
            case CIVILIZATION -> civilization;
            case HISTORY -> history;
            case NAMING -> naming;
        };
    }

    public static String normalizeEndpoint(String value) {
        if (value == null) return "";
        String text = value.trim();
        if (text.isEmpty()) return "";
        if (!text.contains("://")) text = "http://" + text;
        try {
            URI uri = URI.create(text);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return "";
            if (uri.getHost() == null || uri.getHost().isBlank()) return "";
            // Endpoint commands are intentionally credential-free. Reject URL forms that commonly embed secrets.
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) return "";
            String normalized = uri.toString();
            while (normalized.endsWith("/") && normalized.length() > (scheme.length() + 3)) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
