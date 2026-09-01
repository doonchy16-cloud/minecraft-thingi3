package dev.worldmind.ai;

public record AIConnectionResult(boolean success, long latencyMillis, String category, String detail) {
    public AIConnectionResult {
        latencyMillis = Math.max(0, latencyMillis);
        category = sanitize(category, 40);
        detail = sanitize(detail, 120);
    }
    public static AIConnectionResult ok(long latency, String detail) { return new AIConnectionResult(true, latency, "ok", detail); }
    public static AIConnectionResult failed(long latency, String category, String detail) { return new AIConnectionResult(false, latency, category, detail); }
    private static String sanitize(String value, int max) {
        String s = value == null ? "" : value.replace('\n',' ').replace('\r',' ').replace('\t',' ').trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
