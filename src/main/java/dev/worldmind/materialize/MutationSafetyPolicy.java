package dev.worldmind.materialize;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Intentionally conservative: V1 only mutates a small allow-list of stateless building/natural blocks. */
public final class MutationSafetyPolicy {
    public boolean canReplace(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        return isSimpleMutable(state);
    }

    public boolean isSimpleMutable(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.BIRCH_PLANKS) || state.is(Blocks.JUNGLE_PLANKS)
                || state.is(Blocks.ACACIA_PLANKS) || state.is(Blocks.DARK_OAK_PLANKS)
                || state.is(Blocks.MANGROVE_PLANKS) || state.is(Blocks.CHERRY_PLANKS)
                || state.is(Blocks.PALE_OAK_PLANKS);
    }
}
