package dev.worldmind.sim;

public final class SimulationBudget {
    private final int maxUnits;
    private int consumed;

    public SimulationBudget(int maxUnits) {
        if (maxUnits < 0) throw new IllegalArgumentException("maxUnits");
        this.maxUnits = maxUnits;
    }

    public boolean tryConsume() {
        if (consumed >= maxUnits) return false;
        consumed++;
        return true;
    }

    public int consumed() { return consumed; }
    public int remaining() { return Math.max(0, maxUnits - consumed); }
}
