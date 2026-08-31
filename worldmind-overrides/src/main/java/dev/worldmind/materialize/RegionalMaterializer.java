package dev.worldmind.materialize;

import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.protect.ProtectionIndex;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionalOutcome;
import dev.worldmind.state.RegionTransformationPlan;
import dev.worldmind.state.WorldmindState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Materializes abstract regional history only into chunks Minecraft already loaded normally. */
public final class RegionalMaterializer {
    private final WorldmindConfig config;

    public RegionalMaterializer(WorldmindConfig config) { this.config = config.validated(); }

    public Result apply(ServerLevel level, WorldmindState state, RegionTransformationPlan plan) {
        if (plan.status() != dev.worldmind.state.PlanStatus.PENDING) return Result.ALREADY_DONE;
        String dimension = level.dimension().identifier().toString();
        if (!dimension.equals(plan.region().dimension())) return Result.DEFERRED;
        ProtectionIndex protections = new ProtectionIndex(state.protections());
        List<BlockPos> targets = targets(level, plan.region(), plan.seed(), config.maxMutationsPerMaterialization());
        if (targets.isEmpty()) return Result.DEFERRED;

        int changed = 0;
        for (BlockPos pos : targets) {
            if (protections.isProtected(dimension, pos.getX(), pos.getY(), pos.getZ())) continue;
            if (mutate(level, pos, plan.outcome(), plan.seed())) changed++;
        }
        state.markRegionPlanCommitted(plan.id());
        state.region(plan.region()).ifPresent(r -> r.recordMaterializationResult(changed, state.worldTicks()));
        return changed == 0 ? Result.NO_SAFE_MUTATIONS : Result.APPLIED;
    }

    private List<BlockPos> targets(ServerLevel level, RegionKey region, long seed, int max) {
        int candidates = Math.max(24, Math.min(512, max * 4));
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < candidates; i++) {
            long h = mix(seed + i * 0x9e3779b97f4a7c15L);
            int x = region.minBlockX() + Math.floorMod((int)h, RegionKey.CELL_SIZE_BLOCKS);
            int z = region.minBlockZ() + Math.floorMod((int)(h >>> 32), RegionKey.CELL_SIZE_BLOCKS);
            BlockPos probe = new BlockPos(x, 64, z);
            if (!level.hasChunkAt(probe)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos surface = new BlockPos(x, y, z);
            if (!level.hasChunkAt(surface)) continue;
            list.add(surface);
        }
        list.sort(Comparator.comparingLong(p -> mix(seed ^ p.asLong())));
        return List.copyOf(list.subList(0, Math.min(max, list.size())));
    }

    private boolean mutate(ServerLevel level, BlockPos surface, RegionalOutcome outcome, long seed) {
        BlockState ground = level.getBlockState(surface);
        BlockPos above = surface.above();
        BlockState air = level.getBlockState(above);
        long hash = mix(seed ^ surface.asLong());
        return switch (outcome) {
            case STASIS -> false;
            case VEGETATION_SPREAD -> vegetation(level, surface, above, ground, air, hash);
            case VEGETATION_THINNING -> thinning(level, surface, ground, hash);
            case RECLAMATION -> reclaim(level, surface, ground, hash);
            case FARM_SUCCESSION -> farmSuccession(level, surface, above, ground, air, hash);
            case ROUTE_FORMATION -> route(level, surface, ground, hash);
            case VILLAGE_GROWTH -> villageGrowth(level, surface, above, ground, air, hash);
            case VILLAGE_DECLINE -> reclaim(level, surface, ground, hash ^ 0xD3C4A1L);
            case FORTIFICATION -> fortify(level, surface, above, ground, air, hash);
            case PLACE_TRANSFORMATION -> reclaim(level, surface, ground, hash);
            case HISTORIC_REOCCUPATION -> historicReoccupation(level, surface, above, ground, air, hash);
            case ANOMALY_MANIFESTATION -> anomaly(level, surface, above, ground, air, hash);
        };
    }

    private boolean vegetation(ServerLevel level, BlockPos groundPos, BlockPos above, BlockState ground, BlockState air, long h) {
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.PODZOL))) return false;
        if (!air.isAir()) return false;
        if ((h & 15L) == 0L) return level.setBlock(above, Blocks.OAK_SAPLING.defaultBlockState(), 3);
        if ((h & 3L) == 0L && ground.is(Blocks.DIRT)) return level.setBlock(groundPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        return false;
    }

    private boolean thinning(ServerLevel level, BlockPos pos, BlockState ground, long h) {
        if ((h & 3L) != 0L) return false;
        if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL)) return level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
        return false;
    }

    private boolean reclaim(ServerLevel level, BlockPos pos, BlockState ground, long h) {
        if ((h & 1L) != 0L) return false;
        if (ground.is(Blocks.COBBLESTONE)) return level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        if (ground.is(Blocks.STONE_BRICKS)) return level.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
        if (ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT)) return level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), 3);
        return false;
    }

    private boolean farmSuccession(ServerLevel level, BlockPos pos, BlockPos above, BlockState ground, BlockState top, long h) {
        if (!ground.is(Blocks.FARMLAND) || (h & 3L) != 0L) return false;
        if (!top.isAir() && level.getBlockEntity(above) != null) return false;
        if (!top.isAir()) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
        return level.setBlock(pos, ((h & 8L) == 0L ? Blocks.DIRT : Blocks.GRASS_BLOCK).defaultBlockState(), 3);
    }

    private boolean route(ServerLevel level, BlockPos pos, BlockState ground, long h) {
        if ((h & 7L) > 2L) return false;
        if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT))
            return level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
        return false;
    }

    private boolean villageGrowth(ServerLevel level, BlockPos pos, BlockPos above, BlockState ground, BlockState top, long h) {
        if (!top.isAir()) return false;
        if ((h & 15L) == 0L && (ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK)))
            return level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
        if ((h & 31L) == 1L && ground.is(Blocks.COBBLESTONE))
            return level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        return false;
    }

    private boolean fortify(ServerLevel level, BlockPos pos, BlockPos above, BlockState ground, BlockState top, long h) {
        if (!top.isAir() || (h & 31L) != 0L) return false;
        if (ground.is(Blocks.COBBLESTONE) || ground.is(Blocks.STONE) || ground.is(Blocks.STONE_BRICKS))
            return level.setBlock(above, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
        return false;
    }

    private boolean historicReoccupation(ServerLevel level, BlockPos pos, BlockPos above, BlockState ground, BlockState top, long h) {
        if ((h & 7L) == 0L && ground.is(Blocks.COBBLESTONE)) return level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        if ((h & 63L) == 1L && top.isAir() && (ground.is(Blocks.STONE_BRICKS) || ground.is(Blocks.COBBLESTONE)))
            return level.setBlock(above, Blocks.TORCH.defaultBlockState(), 3);
        return false;
    }

    private boolean anomaly(ServerLevel level, BlockPos pos, BlockPos above, BlockState ground, BlockState top, long h) {
        if ((h & 127L) != 0L || !top.isAir()) return false;
        if (ground.is(Blocks.STONE) || ground.is(Blocks.DEEPSLATE) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT))
            return level.setBlock(above, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        return false;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }

    public enum Result { APPLIED, DEFERRED, ALREADY_DONE, NO_SAFE_MUTATIONS }
}
