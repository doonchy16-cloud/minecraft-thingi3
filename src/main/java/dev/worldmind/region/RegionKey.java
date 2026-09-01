package dev.worldmind.region;

import java.util.Objects;

public record RegionKey(String dimension, int cellX, int cellZ) {
    public static final int CELL_SIZE_BLOCKS = 128;

    public RegionKey {
        dimension = Objects.requireNonNull(dimension, "dimension");
    }

    public static RegionKey fromBlock(String dimension, int blockX, int blockZ) {
        return new RegionKey(dimension, Math.floorDiv(blockX, CELL_SIZE_BLOCKS), Math.floorDiv(blockZ, CELL_SIZE_BLOCKS));
    }

    public int minBlockX() { return cellX * CELL_SIZE_BLOCKS; }
    public int minBlockZ() { return cellZ * CELL_SIZE_BLOCKS; }
    public int centerBlockX() { return minBlockX() + CELL_SIZE_BLOCKS / 2; }
    public int centerBlockZ() { return minBlockZ() + CELL_SIZE_BLOCKS / 2; }
    public String stableId() { return dimension + "@" + cellX + "," + cellZ; }
}
