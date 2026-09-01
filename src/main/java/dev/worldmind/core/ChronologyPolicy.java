package dev.worldmind.core;

/** Converts offline wall-clock time into bounded abstract Worldmind time. */
public final class ChronologyPolicy {
    public static final long MINECRAFT_DAY_TICKS = 24_000L;
    public static final long MAX_CATCH_UP_TICKS = 30L * MINECRAFT_DAY_TICKS;
    private static final long REAL_DAY_MILLIS = 86_400_000L;

    public CatchUpResult compute(ChronologyMode mode, long offlineMillis) {
        long elapsed = Math.max(0L, offlineMillis);
        return switch (mode) {
            case PAUSED -> new CatchUpResult(0L, false);
            case LIVING -> new CatchUpResult(elapsed / 50L, false);
            case CAPPED_LIVING -> cappedLiving(elapsed);
        };
    }

    private CatchUpResult cappedLiving(long offlineMillis) {
        double realDays = offlineMillis / (double) REAL_DAY_MILLIS;
        if (realDays <= 0.0) return new CatchUpResult(0L, false);

        // Strong early return-home effect, then diminishing conversion.
        double effectiveMinecraftDays;
        if (realDays <= 1.0) {
            effectiveMinecraftDays = realDays * 5.0;
        } else if (realDays <= 7.0) {
            effectiveMinecraftDays = 5.0 + (realDays - 1.0) * 2.0;
        } else {
            effectiveMinecraftDays = 17.0 + Math.log1p(realDays - 7.0) * 3.0;
        }
        long ticks = Math.round(effectiveMinecraftDays * MINECRAFT_DAY_TICKS);
        boolean capped = ticks > MAX_CATCH_UP_TICKS;
        return new CatchUpResult(Math.min(ticks, MAX_CATCH_UP_TICKS), capped);
    }
}
