package dev.worldmind.ai;

import java.util.Optional;

@FunctionalInterface
public interface AIAdviceSource {
    Optional<AIValidatedAdvice> adviceFor(String placeId);

    default boolean planningPending(String placeId) { return false; }

    static AIAdviceSource none() { return id -> Optional.empty(); }
}
