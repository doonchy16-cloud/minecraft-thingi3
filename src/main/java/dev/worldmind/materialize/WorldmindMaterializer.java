package dev.worldmind.materialize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.content.WorldmindBlocks;
import dev.worldmind.core.EvolutionType;
import dev.worldmind.state.PlaceRecord;
import dev.worldmind.state.TransformationPlan;

public final class WorldmindMaterializer {
    private final WorldmindConfig config;
    private final MutationSafetyPolicy safety = new MutationSafetyPolicy();

    public WorldmindMaterializer(WorldmindConfig config) { this.config = config; }

    public Result apply(ServerLevel level, PlaceRecord place, TransformationPlan plan) {
        if (plan.status() != dev.worldmind.state.PlanStatus.PENDING) return Result.ALREADY_DONE;
        if (hasWorldseal(level, place)) {
            place.setSealed(true);
            plan.markDeferred();
            return Result.PROTECTED;
        }
        if (!level.hasChunkAt(new BlockPos(place.x(), place.y(), place.z()))) return Result.DEFERRED;

        List<BlockPos> targets = deterministicTargets(place, plan.seed(), config.maxMutationsPerMaterialization());
        int changed = 0;
        for (BlockPos pos : targets) {
            if (!level.hasChunkAt(pos)) continue;
            if (applyAt(level, place, plan, pos)) changed++;
        }
        if (plan.type() == EvolutionType.CONSTRUCTIVE && plan.intensity() >= 0.55
                && place.structureProfile().architecturalConfidence() >= 0.72) {
            changed += applyConstructiveExpansion(level, place, plan);
        }
        plan.markCommitted();
        return changed == 0 ? Result.NO_SAFE_MUTATIONS : Result.APPLIED;
    }

    private boolean applyAt(ServerLevel level, PlaceRecord place, TransformationPlan plan, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        EvolutionType type = plan.type();
        if (type == EvolutionType.BLENDED) {
            type = ((positionHash(pos, plan.seed()) & 1L) == 0L) ? EvolutionType.RECLAMATION : EvolutionType.CONSTRUCTIVE;
        }
        return switch (type) {
            case STASIS -> false;
            case DECAY -> decay(level, pos, current);
            case CONSTRUCTIVE -> constructive(level, pos, current, place);
            case RECLAMATION -> reclamation(level, pos, current);
            case BLENDED -> false;
        };
    }

    private boolean decay(ServerLevel level, BlockPos pos, BlockState current) {
        if (!safety.canReplace(level, pos)) return false;
        if ((positionHash(pos, 0xdecadeL) & 7L) > 2L) return false;
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private boolean constructive(ServerLevel level, BlockPos pos, BlockState current, PlaceRecord place) {
        if (pos.getY() < place.y() - 1) return false;
        if (!safety.canReplace(level, pos)) return false;
        if (!(current.is(Blocks.DIRT) || current.is(Blocks.COARSE_DIRT) || current.is(Blocks.GRASS_BLOCK)
                || current.is(Blocks.COBBLESTONE) || current.is(Blocks.OAK_PLANKS))) return false;
        BlockState upgraded = (current.is(Blocks.OAK_PLANKS)) ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
        if (current.equals(upgraded)) return false;
        return level.setBlock(pos, upgraded, 3);
    }

    private boolean reclamation(ServerLevel level, BlockPos pos, BlockState current) {
        if (current.is(Blocks.DIRT) || current.is(Blocks.COARSE_DIRT) || current.is(Blocks.GRASS_BLOCK)) {
            if (!safety.canReplace(level, pos)) return false;
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            return level.setBlock(pos, moss, 3);
        }
        if (current.is(Blocks.COBBLESTONE) && safety.canReplace(level, pos)) {
            return level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        }
        return false;
    }

    private int applyConstructiveExpansion(ServerLevel level, PlaceRecord place, TransformationPlan plan) {
        int r = Math.min(9, Math.max(5, place.radius() - 1));
        int baseY = place.y() - 1;
        int changed = 0;
        BlockState pillar = place.structureProfile().dominantPalette().equals("wood")
                ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
        int[][] corners = {{-r,-r},{r,-r},{-r,r},{r,r}};
        for (int[] c : corners) {
            int x = place.x()+c[0], z = place.z()+c[1];
            BlockPos ground = new BlockPos(x, baseY, z);
            if (!level.hasChunkAt(ground)) continue;
            if (level.getBlockState(ground).isAir()) continue;
            for (int h=1; h<=4; h++) {
                BlockPos p = ground.above(h);
                if (!level.hasChunkAt(p) || !level.getBlockState(p).isAir()) continue;
                if (level.setBlock(p, pillar, 3)) changed++;
            }
        }
        // Sparse perimeter makes the intervention read as a fortification without enclosing/rewriting the build.
        for (int d=-r+1; d<r; d+=2) {
            changed += placeWallPost(level, new BlockPos(place.x()+d, baseY, place.z()-r));
            changed += placeWallPost(level, new BlockPos(place.x()+d, baseY, place.z()+r));
            changed += placeWallPost(level, new BlockPos(place.x()-r, baseY, place.z()+d));
            changed += placeWallPost(level, new BlockPos(place.x()+r, baseY, place.z()+d));
            if (changed >= Math.max(8, config.maxMutationsPerMaterialization()/3)) break;
        }
        return changed;
    }

    private int placeWallPost(ServerLevel level, BlockPos ground) {
        if (!level.hasChunkAt(ground) || level.getBlockState(ground).isAir()) return 0;
        BlockPos p = ground.above();
        if (!level.hasChunkAt(p) || !level.getBlockState(p).isAir()) return 0;
        return level.setBlock(p, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3) ? 1 : 0;
    }

    private boolean hasWorldseal(ServerLevel level, PlaceRecord place) {
        int r = Math.max(place.radius(), config.worldsealRadius());
        int vertical = Math.min(12, r);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((long)dx * dx + (long)dz * dz > (long)r * r) continue;
                for (int dy = -vertical; dy <= vertical; dy++) {
                    BlockPos p = new BlockPos(place.x() + dx, place.y() + dy, place.z() + dz);
                    if (!level.hasChunkAt(p)) continue;
                    if (level.getBlockState(p).is(WorldmindBlocks.WORLDSEAL)) return true;
                }
            }
        }
        return false;
    }

    private List<BlockPos> deterministicTargets(PlaceRecord place, long seed, int max) {
        int r = Math.min(12, Math.max(5, place.radius()));
        List<BlockPos> all = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 7; dy++) {
                    if (Math.abs(dx) < r - 2 && Math.abs(dz) < r - 2 && dy < 0) continue;
                    all.add(new BlockPos(place.x() + dx, place.y() + dy, place.z() + dz));
                }
            }
        }
        all.sort(Comparator.comparingLong(p -> positionHash(p, seed)));
        int scaled = Math.max(1, (int)Math.round(max * Math.max(0.15, Math.min(1.0, 0.25 + place.confidence() * 0.75))));
        return all.subList(0, Math.min(scaled, all.size()));
    }

    private static long positionHash(BlockPos p, long seed) {
        long z = seed ^ ((long)p.getX() * 341873128712L) ^ ((long)p.getY() * 132897987541L) ^ ((long)p.getZ() * 42317861L);
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }

    public enum Result { APPLIED, PROTECTED, DEFERRED, ALREADY_DONE, NO_SAFE_MUTATIONS }
}
