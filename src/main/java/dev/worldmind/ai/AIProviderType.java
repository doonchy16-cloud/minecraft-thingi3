package dev.worldmind.ai;

import java.util.Locale;

public enum AIProviderType {
    BUILTIN,
    OLLAMA,
    FORGEY,
    COMPATIBLE;

    public static AIProviderType parse(String value) {
        if (value == null || value.isBlank()) return BUILTIN;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return BUILTIN; }
    }
}
