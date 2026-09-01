package dev.worldmind.region;

public final class SpatialCell {
    private double vegetation;
    private double forest;
    private double farm;
    private double settlement;
    private double disturbance;
    private double route;
    private double reclamation;
    private double build;
    private double protection;
    private long lastTouchedTick;

    public double signal(SpatialSignal signal) {
        return switch (signal) {
            case VEGETATION -> vegetation;
            case FOREST -> forest;
            case FARM -> farm;
            case SETTLEMENT -> settlement;
            case DISTURBANCE -> disturbance;
            case ROUTE -> route;
            case RECLAMATION -> reclamation;
            case BUILD -> build;
            case PROTECTION -> protection;
        };
    }

    public void add(SpatialSignal signal, double amount) {
        double next = clamp(signal(signal) + amount);
        switch (signal) {
            case VEGETATION -> vegetation = next;
            case FOREST -> forest = next;
            case FARM -> farm = next;
            case SETTLEMENT -> settlement = next;
            case DISTURBANCE -> disturbance = next;
            case ROUTE -> route = next;
            case RECLAMATION -> reclamation = next;
            case BUILD -> build = next;
            case PROTECTION -> protection = next;
        }
    }

    public void set(SpatialSignal signal, double value) {
        double next = clamp(value);
        switch (signal) {
            case VEGETATION -> vegetation = next;
            case FOREST -> forest = next;
            case FARM -> farm = next;
            case SETTLEMENT -> settlement = next;
            case DISTURBANCE -> disturbance = next;
            case ROUTE -> route = next;
            case RECLAMATION -> reclamation = next;
            case BUILD -> build = next;
            case PROTECTION -> protection = next;
        }
    }

    public void age(double days) {
        if (days <= 0) return;
        route *= Math.exp(-days / 80.0);
        disturbance *= Math.exp(-days / 160.0);
        reclamation = clamp(reclamation + Math.max(vegetation, forest) * days * 0.0025);
    }

    public long lastTouchedTick() { return lastTouchedTick; }
    public void markTouched(long tick) { lastTouchedTick = Math.max(lastTouchedTick, tick); }

    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
