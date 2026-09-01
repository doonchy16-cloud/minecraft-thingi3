package dev.worldmind.diagnostic;

import dev.worldmind.WorldmindMod;
import dev.worldmind.config.WorldmindConfigLoader;

public final class WorldmindDiagnostics {
    private static volatile Boolean runtimeDebugOverride;
    private WorldmindDiagnostics() {}

    public static void debug(String message, Object... args) {
        if (debugEnabled()) WorldmindMod.LOGGER.info("[debug] " + message, args);
    }

    public static boolean debugEnabled() {
        Boolean override = runtimeDebugOverride;
        return override != null ? override : WorldmindConfigLoader.get().debugLogging();
    }

    public static void setRuntimeDebug(boolean enabled) { runtimeDebugOverride = enabled; }
}
