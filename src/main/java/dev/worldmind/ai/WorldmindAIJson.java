package dev.worldmind.ai;

import java.util.Locale;

final class WorldmindAIJson {
    private WorldmindAIJson() {}

    static String quote(String value) {
        if (value == null) return "\"\"";
        StringBuilder out = new StringBuilder(value.length() + 8).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    static String stringValue(String json, String key) {
        int p = valueStart(json, key);
        if (p < 0 || p >= json.length() || json.charAt(p) != '"') return null;
        StringBuilder out = new StringBuilder();
        for (int i = p + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (++i >= json.length()) return null;
            char e = json.charAt(i);
            switch (e) {
                case '"', '\\', '/' -> out.append(e);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 >= json.length()) return null;
                    try { out.append((char)Integer.parseInt(json.substring(i + 1, i + 5), 16)); }
                    catch (NumberFormatException ex) { return null; }
                    i += 4;
                }
                default -> { return null; }
            }
        }
        return null;
    }

    static Double doubleValue(String json, String key) {
        int p = valueStart(json, key);
        if (p < 0) return null;
        int end = p;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (!(c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || Character.isDigit(c))) break;
            end++;
        }
        if (end == p) return null;
        try { return Double.parseDouble(json.substring(p, end)); }
        catch (NumberFormatException e) { return null; }
    }

    static AITransformationProposal parseProposal(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("empty-ai-proposal");
        String placeId = stringValue(json, "placeId");
        String recommendation = stringValue(json, "recommendation");
        Double confidence = doubleValue(json, "confidence");
        Double adjustment = doubleValue(json, "intensityAdjustment");
        String reason = stringValue(json, "reason");
        String style = stringValue(json, "styleHint");
        if (placeId == null || recommendation == null || confidence == null) {
            throw new IllegalArgumentException("malformed-ai-proposal");
        }
        return new AITransformationProposal(placeId, recommendation, confidence,
                adjustment == null ? 0.0 : adjustment, reason == null ? "" : reason, style == null ? "" : style);
    }

    static String contextJson(AIPlaceContext c) {
        return "{" +
                "\"placeId\":" + quote(c.placeId()) + "," +
                "\"placeKind\":" + quote(c.placeKind()) + "," +
                "\"placeConfidence\":" + number(c.placeConfidence()) + "," +
                "\"dominantPalette\":" + quote(c.dominantPalette()) + "," +
                "\"defensiveIntent\":" + number(c.defensiveIntent()) + "," +
                "\"unfinishedIntent\":" + number(c.unfinishedIntent()) + "," +
                "\"expansionIntent\":" + number(c.expansionIntent()) + "," +
                "\"absenceDays\":" + number(c.absenceDays()) + "," +
                "\"naturePressure\":" + number(c.naturePressure()) + "," +
                "\"settlementPressure\":" + number(c.settlementPressure()) + "," +
                "\"structuralFragility\":" + number(c.structuralFragility()) + "," +
                "\"threatPressure\":" + number(c.threatPressure()) + "," +
                "\"sealed\":" + c.sealed() + "," +
                "\"deterministicRecommendation\":" + quote(c.deterministicRecommendation().name()) + "," +
                "\"deterministicIntensity\":" + number(c.deterministicIntensity()) + "}";
    }

    private static String number(double v) { return Double.toString(Double.isFinite(v) ? v : 0.0); }

    private static int valueStart(String json, String key) {
        if (json == null || key == null) return -1;
        String needle = "\"" + key + "\"";
        int from = 0;
        while (from < json.length()) {
            int k = json.indexOf(needle, from);
            if (k < 0) return -1;
            int p = k + needle.length();
            while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
            if (p < json.length() && json.charAt(p) == ':') {
                p++;
                while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
                return p;
            }
            from = k + needle.length();
        }
        return -1;
    }
}
