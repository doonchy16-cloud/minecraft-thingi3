package dev.worldmind.ai;

import java.util.Locale;

public enum AIFeature {
    STRUCTURE,
    TRANSFORMATION,
    CIVILIZATION,
    HISTORY,
    NAMING;

    public static AIFeature parse(String value) {
        if (value == null) throw new IllegalArgumentException("feature");
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
