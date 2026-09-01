package dev.worldmind.materialize;

import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.protect.ProtectionIndex;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.RegionalOutcome;
import dev.worldmind.region.SpatialField;
import dev.worldmind.region.SpatialProcess;
import dev.worldmind.region.SpatialSignal;
import dev.worldmind.region.SpatialProcessPlanner;
import dev.worldmind.state.RegionTransformationPlan;
import dev.worldmind.state.WorldmindState;
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

        SpatialProcess process = plan.process() == SpatialProcess.NONE ? fallbackProcess(plan.outcome()) : plan.process();
        int[] cells = plan.spatialCells();
        if (cells.length == 0) cells = new int[] { Math.floorMod((int)plan.seed(), SpatialField.GRID_SIZE * SpatialField.GRID_SIZE) };
        int storedBudget = plan.targetMutations() > 0
                ? Math.min(config.maxMutationsPerMaterialization(), plan.targetMutations())
                : Math.max(16, (int)Math.round(config.maxMutationsPerMaterialization() * Math.max(.15, plan.intensity())));
        double currentPhysicalAge = state.region(plan.region()).map(r -> r.snapshot(state.worldTicks()).elapsedDays()).orElse(0.0);
        int agedBudget = new SpatialProcessPlanner().magnitude(currentPhysicalAge, plan.intensity(), config.maxMutationsPerMaterialization());
        int budget = Math.max(storedBudget, agedBudget);
        List<SpatialMaterializationGeometry.XZ> geometry = SpatialMaterializationGeometry.targets(
                plan.region(), process, cells, plan.seed(), budget);
        if (geometry.isEmpty()) return Result.DEFERRED;

        ProtectionIndex protections = new ProtectionIndex(state.protections());
        int changed = 0;
        BiomeVegetationPalette.Palette palette = null;
        for (int i = 0; i < geometry.size() && changed < budget; i++) {
            SpatialMaterializationGeometry.XZ xz = geometry.get(i);
            BlockPos probe = new BlockPos(xz.x(), 64, xz.z());
            if (!level.hasChunkAt(probe)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.x(), xz.z()) - 1;
            BlockPos surface = new BlockPos(xz.x(), y, xz.z());
            if (!level.hasChunkAt(surface)) continue;
            if (protections.isProtected(dimension, surface.getX(), surface.getY(), surface.getZ())) continue;
            if (palette == null && isVegetationProcess(process)) palette = BiomeVegetationPalette.choose(level, surface);
            changed += mutate(level, protections, dimension, surface, process, plan.outcome(), plan.seed() ^ i, palette, budget - changed);
        }

        state.markRegionPlanCommitted(plan.id());
        final int changedBlocks = changed;
        final int[] affectedCells = cells;
        state.region(plan.region()).ifPresent(region -> {
            region.recordMaterializationResult(changedBlocks, state.worldTicks());
            if (changedBlocks > 0) {
                for (int cellIndex : affectedCells) {
                    var cell = region.spatial().cell(region.spatial().x(cellIndex), region.spatial().z(cellIndex));
                    cell.markTouched(state.worldTicks());
                    switch (process) {
                        case FOREST_FRONTIER, VEGETATION_PATCH -> { cell.add(SpatialSignal.FOREST, .08); cell.add(SpatialSignal.VEGETATION, .08); }
                        case ROUTE_CORRIDOR -> cell.add(SpatialSignal.ROUTE, .08);
                        case FARM_SUCCESSION_PATCH -> { cell.add(SpatialSignal.FARM, -.12); cell.add(SpatialSignal.RECLAMATION, .10); }
                        case SETTLEMENT_GROWTH_EDGE, FORTIFICATION_EDGE -> cell.add(SpatialSignal.SETTLEMENT, .06);
                        case RECLAMATION_PATCH, SETTLEMENT_DECLINE_PATCH, PLACE_DOMAIN -> cell.add(SpatialSignal.RECLAMATION, .09);
                        default -> { }
                    }
                }
            }
        });
        return changed == 0 ? Result.NO_SAFE_MUTATIONS : Result.APPLIED;
    }

    private int mutate(ServerLevel level, ProtectionIndex protections, String dimension, BlockPos surface,
            SpatialProcess process, RegionalOutcome outcome, long seed, BiomeVegetationPalette.Palette palette, int remainingBudget) {
        if (remainingBudget <= 0) return 0;
        return switch (process) {
            case FOREST_FRONTIER -> growTree(level, protections, dimension, surface, palette, seed, remainingBudget);
            case VEGETATION_PATCH -> vegetation(level, protections, dimension, surface, outcome, palette, seed, remainingBudget);
            case RECLAMATION_PATCH, SETTLEMENT_DECLINE_PATCH, PLACE_DOMAIN -> reclaim(level, surface, seed);
            case FARM_SUCCESSION_PATCH -> farmSuccession(level, surface, palette, protections, dimension, seed, remainingBudget);
            case ROUTE_CORRIDOR -> route(level, surface, planWidth(seed), remainingBudget);
            case SETTLEMENT_GROWTH_EDGE -> settlementGrowth(level, surface, seed, remainingBudget);
            case FORTIFICATION_EDGE -> fortify(level, protections, dimension, surface, seed, remainingBudget);
            case HISTORIC_REOCCUPATION_SITE -> historicReoccupation(level, surface, seed, remainingBudget);
            case ANOMALY_SITE -> anomaly(level, protections, dimension, surface, seed);
            case NONE -> 0;
        };
    }

    private int vegetation(ServerLevel level, ProtectionIndex protections, String dimension, BlockPos surface,
            RegionalOutcome outcome, BiomeVegetationPalette.Palette palette, long seed, int remainingBudget) {
        if (outcome == RegionalOutcome.VEGETATION_THINNING) {
            BlockState ground = level.getBlockState(surface);
            if ((ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL)) && (mix(seed) & 3L) <= 1L)
                return level.setBlock(surface, Blocks.COARSE_DIRT.defaultBlockState(), 3) ? 1 : 0;
            return 0;
        }
        if ((mix(seed) & 3L) != 0L) {
            BlockState ground = level.getBlockState(surface);
            if (ground.is(Blocks.DIRT)) return level.setBlock(surface, Blocks.GRASS_BLOCK.defaultBlockState(), 3) ? 1 : 0;
            return 0;
        }
        return growTree(level, protections, dimension, surface, palette, seed, remainingBudget);
    }

    private int growTree(ServerLevel level, ProtectionIndex protections, String dimension, BlockPos surface,
            BiomeVegetationPalette.Palette palette, long seed, int remainingBudget) {
        if (palette == null || remainingBudget < 4) return 0;
        BlockState ground = level.getBlockState(surface);
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.PODZOL) || ground.is(Blocks.MOSS_BLOCK))) return 0;
        int height = 3 + (int)(mix(seed) & 1L);
        for (int y=1; y<=height+2; y++) {
            BlockPos p = surface.above(y);
            if (protections.isProtected(dimension,p.getX(),p.getY(),p.getZ()) || !level.getBlockState(p).isAir()) return 0;
        }
        int changed = 0;
        for (int y=1; y<=height && changed < remainingBudget; y++) {
            BlockPos p = surface.above(y);
            if (level.setBlock(p, palette.log().defaultBlockState(), 3)) changed++;
        }
        for (int dy=-1; dy<=1 && changed < remainingBudget; dy++) {
            int radius = dy == 1 ? 1 : 2;
            int canopyY = surface.getY() + height + dy;
            for (int dx=-radius; dx<=radius && changed < remainingBudget; dx++) {
                for (int dz=-radius; dz<=radius && changed < remainingBudget; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && (mix(seed ^ dx * 31L ^ dz * 17L ^ dy) & 1L) == 0L) continue;
                    BlockPos p = new BlockPos(surface.getX()+dx, canopyY, surface.getZ()+dz);
                    if (protections.isProtected(dimension,p.getX(),p.getY(),p.getZ())) continue;
                    if (!level.getBlockState(p).isAir()) continue;
                    if (level.setBlock(p, palette.leaves().defaultBlockState(), 3)) changed++;
                }
            }
        }
        return changed;
    }

    private int reclaim(ServerLevel level, BlockPos pos, long seed) {
        BlockState ground = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null) return 0;
        if (ground.is(Blocks.COBBLESTONE)) return level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3) ? 1 : 0;
        if (ground.is(Blocks.STONE_BRICKS)) return level.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3) ? 1 : 0;
        if ((ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT)) && (mix(seed) & 1L) == 0L)
            return level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), 3) ? 1 : 0;
        return 0;
    }

    private int farmSuccession(ServerLevel level, BlockPos surface, BiomeVegetationPalette.Palette palette,
            ProtectionIndex protections, String dimension, long seed, int remainingBudget) {
        BlockPos farm = level.getBlockState(surface).is(Blocks.FARMLAND) ? surface : surface.below();
        if (!level.getBlockState(farm).is(Blocks.FARMLAND) || protections.isProtected(dimension,farm.getX(),farm.getY(),farm.getZ())) return 0;
        BlockPos top = farm.above();
        if (level.getBlockEntity(top) != null) return 0;
        int changed = 0;
        if (!level.getBlockState(top).isAir() && level.setBlock(top, Blocks.AIR.defaultBlockState(), 3)) changed++;
        if (changed < remainingBudget && level.setBlock(farm, ((mix(seed)&1L)==0L?Blocks.DIRT:Blocks.GRASS_BLOCK).defaultBlockState(),3)) changed++;
        if ((mix(seed) & 7L) == 0L && changed + 4 < remainingBudget) changed += growTree(level, protections, dimension, farm, palette, seed ^ 0xFA12L, remainingBudget - changed);
        return changed;
    }

    private int route(ServerLevel level, BlockPos surface, int width, int remainingBudget) {
        int changed = 0;
        for (int dx=-width; dx<=width && changed < remainingBudget; dx++) {
            for (int dz=-width; dz<=width && changed < remainingBudget; dz++) {
                BlockPos p = surface.offset(dx,0,dz);
                if (!level.hasChunkAt(p) || level.getBlockEntity(p) != null) continue;
                BlockState ground = level.getBlockState(p);
                if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.PODZOL))
                    if (level.setBlock(p, Blocks.DIRT_PATH.defaultBlockState(), 3)) changed++;
            }
        }
        return changed;
    }

    private int settlementGrowth(ServerLevel level, BlockPos surface, long seed, int remainingBudget) {
        BlockState ground = level.getBlockState(surface);
        BlockPos above = surface.above();
        if (level.getBlockEntity(surface) != null || level.getBlockEntity(above) != null) return 0;
        if ((ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT)) && (mix(seed)&3L) != 3L)
            return level.setBlock(surface, Blocks.DIRT_PATH.defaultBlockState(), 3) ? 1 : 0;
        if (remainingBudget >= 2 && level.getBlockState(above).isAir() && (ground.is(Blocks.COBBLESTONE) || ground.is(Blocks.STONE_BRICKS))) {
            int changed = level.setBlock(above, Blocks.COBBLESTONE_WALL.defaultBlockState(),3) ? 1 : 0;
            if (changed > 0 && remainingBudget > 1 && level.getBlockState(above.above()).isAir() && (mix(seed)&7L)==0L)
                changed += level.setBlock(above.above(), Blocks.TORCH.defaultBlockState(),3) ? 1 : 0;
            return changed;
        }
        return 0;
    }

    private int fortify(ServerLevel level, ProtectionIndex protections, String dimension, BlockPos surface, long seed, int remainingBudget) {
        if (remainingBudget < 1) return 0;
        BlockPos above = surface.above();
        if (protections.isProtected(dimension,above.getX(),above.getY(),above.getZ()) || !level.getBlockState(above).isAir()) return 0;
        BlockState ground = level.getBlockState(surface);
        if (ground.is(Blocks.COBBLESTONE) || ground.is(Blocks.STONE) || ground.is(Blocks.STONE_BRICKS) || ground.is(Blocks.GRASS_BLOCK))
            return level.setBlock(above, Blocks.COBBLESTONE_WALL.defaultBlockState(),3) ? 1 : 0;
        return 0;
    }

    private int historicReoccupation(ServerLevel level, BlockPos surface, long seed, int remainingBudget) {
        BlockState ground = level.getBlockState(surface);
        BlockPos above = surface.above();
        if (ground.is(Blocks.COBBLESTONE)) return level.setBlock(surface, Blocks.STONE_BRICKS.defaultBlockState(),3) ? 1 : 0;
        if (remainingBudget > 0 && level.getBlockState(above).isAir() && (ground.is(Blocks.STONE_BRICKS) || ground.is(Blocks.COBBLESTONE)) && (mix(seed)&7L)==0L)
            return level.setBlock(above, Blocks.TORCH.defaultBlockState(),3) ? 1 : 0;
        return 0;
    }

    private int anomaly(ServerLevel level, ProtectionIndex protections, String dimension, BlockPos surface, long seed) {
        BlockPos above = surface.above();
        if ((mix(seed)&31L) != 0L || !level.getBlockState(above).isAir() || protections.isProtected(dimension,above.getX(),above.getY(),above.getZ())) return 0;
        BlockState ground = level.getBlockState(surface);
        if (ground.is(Blocks.STONE) || ground.is(Blocks.DEEPSLATE) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT))
            return level.setBlock(above, Blocks.CRYING_OBSIDIAN.defaultBlockState(),3) ? 1 : 0;
        return 0;
    }

    private static boolean isVegetationProcess(SpatialProcess process) {
        return process == SpatialProcess.FOREST_FRONTIER || process == SpatialProcess.VEGETATION_PATCH
                || process == SpatialProcess.FARM_SUCCESSION_PATCH;
    }

    private static int planWidth(long seed) { return (mix(seed)&7L)==0L ? 1 : 0; }

    private static SpatialProcess fallbackProcess(RegionalOutcome outcome) {
        return switch (outcome) {
            case STASIS -> SpatialProcess.NONE;
            case VEGETATION_SPREAD, VEGETATION_THINNING -> SpatialProcess.VEGETATION_PATCH;
            case RECLAMATION -> SpatialProcess.RECLAMATION_PATCH;
            case FARM_SUCCESSION -> SpatialProcess.FARM_SUCCESSION_PATCH;
            case ROUTE_FORMATION -> SpatialProcess.ROUTE_CORRIDOR;
            case VILLAGE_GROWTH -> SpatialProcess.SETTLEMENT_GROWTH_EDGE;
            case VILLAGE_DECLINE -> SpatialProcess.SETTLEMENT_DECLINE_PATCH;
            case FORTIFICATION -> SpatialProcess.FORTIFICATION_EDGE;
            case PLACE_TRANSFORMATION -> SpatialProcess.PLACE_DOMAIN;
            case HISTORIC_REOCCUPATION -> SpatialProcess.HISTORIC_REOCCUPATION_SITE;
            case ANOMALY_MANIFESTATION -> SpatialProcess.ANOMALY_SITE;
        };
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }

    public enum Result { APPLIED, DEFERRED, ALREADY_DONE, NO_SAFE_MUTATIONS }
}
