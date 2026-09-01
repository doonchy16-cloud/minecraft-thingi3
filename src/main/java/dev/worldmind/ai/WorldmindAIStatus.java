package dev.worldmind.ai;

public record WorldmindAIStatus(
        boolean enabled,
        AIProviderType provider,
        String endpoint,
        String model,
        int inFlight,
        int cachedAdvice,
        AIConnectionResult lastTest) {}
