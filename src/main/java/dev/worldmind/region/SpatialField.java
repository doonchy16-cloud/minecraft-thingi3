package dev.worldmind.region;

import java.util.ArrayList;
import java.util.List;

public final class SpatialField {
    public static final int GRID_SIZE = 8;
    public static final int MICRO_SIZE_BLOCKS = RegionKey.CELL_SIZE_BLOCKS / GRID_SIZE;
    private SpatialCell[] cells;

    public SpatialField() { ensure(); }

    public SpatialCell cell(int x, int z) {
        ensure();
        if (x < 0 || z < 0 || x >= GRID_SIZE || z >= GRID_SIZE) throw new IndexOutOfBoundsException(x + "," + z);
        return cells[z * GRID_SIZE + x];
    }

    public void add(int x, int z, SpatialSignal signal, double amount) { cell(x,z).add(signal, amount); }
    public void set(int x, int z, SpatialSignal signal, double value) { cell(x,z).set(signal, value); }

    public int cellXForBlock(RegionKey key, int blockX) {
        return Math.max(0, Math.min(GRID_SIZE - 1, Math.floorDiv(blockX - key.minBlockX(), MICRO_SIZE_BLOCKS)));
    }

    public int cellZForBlock(RegionKey key, int blockZ) {
        return Math.max(0, Math.min(GRID_SIZE - 1, Math.floorDiv(blockZ - key.minBlockZ(), MICRO_SIZE_BLOCKS)));
    }

    public void addAtBlock(RegionKey key, int blockX, int blockZ, SpatialSignal signal, double amount) {
        add(cellXForBlock(key, blockX), cellZForBlock(key, blockZ), signal, amount);
    }

    public List<Integer> cardinalNeighbors(int x, int z) {
        List<Integer> out = new ArrayList<>(4);
        if (x > 0) out.add(index(x - 1, z));
        if (x + 1 < GRID_SIZE) out.add(index(x + 1, z));
        if (z > 0) out.add(index(x, z - 1));
        if (z + 1 < GRID_SIZE) out.add(index(x, z + 1));
        return List.copyOf(out);
    }

    public void age(double days) {
        ensure();
        for (SpatialCell cell : cells) cell.age(days);
    }

    public int strongest(SpatialSignal signal) {
        ensure();
        int best = 0;
        double score = -1;
        for (int i = 0; i < cells.length; i++) {
            double value = cells[i].signal(signal);
            if (value > score) { score = value; best = i; }
        }
        return best;
    }

    public List<Integer> strongestCells(SpatialSignal signal, int limit, double minimum) {
        ensure();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) if (cells[i].signal(signal) >= minimum) indexes.add(i);
        indexes.sort((a,b) -> Double.compare(cells[b].signal(signal), cells[a].signal(signal)));
        return List.copyOf(indexes.subList(0, Math.min(Math.max(0, limit), indexes.size())));
    }

    public int index(int x, int z) { return z * GRID_SIZE + x; }
    public int x(int index) { return Math.floorMod(index, GRID_SIZE); }
    public int z(int index) { return Math.floorDiv(index, GRID_SIZE); }
    public int centerBlockX(RegionKey key, int index) { return key.minBlockX() + x(index) * MICRO_SIZE_BLOCKS + MICRO_SIZE_BLOCKS / 2; }
    public int centerBlockZ(RegionKey key, int index) { return key.minBlockZ() + z(index) * MICRO_SIZE_BLOCKS + MICRO_SIZE_BLOCKS / 2; }

    private void ensure() {
        if (cells == null || cells.length != GRID_SIZE * GRID_SIZE) cells = new SpatialCell[GRID_SIZE * GRID_SIZE];
        for (int i = 0; i < cells.length; i++) if (cells[i] == null) cells[i] = new SpatialCell();
    }
}
