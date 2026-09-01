package dev.worldmind.core;

public record CatchUpResult(long abstractTicks, boolean capped) {
    public CatchUpResult {
        abstractTicks = Math.max(0L, abstractTicks);
    }
}
