package dev.worldmind.materialize;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Chooses vegetation from nearby already-loaded tree evidence instead of assuming oak everywhere. */
public final class BiomeVegetationPalette {
    private BiomeVegetationPalette() {}

    public static Palette choose(ServerLevel level, BlockPos origin) {
        int jungle=0,spruce=0,birch=0,acacia=0,darkOak=0,mangrove=0,cherry=0,oak=0;
        for (int dx=-16; dx<=16; dx+=8) for (int dz=-16; dz<=16; dz+=8) {
            BlockPos probe = origin.offset(dx,0,dz);
            if (!level.hasChunkAt(probe)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe.getX(), probe.getZ()) - 1;
            for (int dy=1; dy<=10; dy++) {
                BlockState s = level.getBlockState(new BlockPos(probe.getX(), y+dy, probe.getZ()));
                if (s.is(Blocks.JUNGLE_LOG) || s.is(Blocks.JUNGLE_WOOD)) jungle++;
                else if (s.is(Blocks.SPRUCE_LOG) || s.is(Blocks.SPRUCE_WOOD)) spruce++;
                else if (s.is(Blocks.BIRCH_LOG) || s.is(Blocks.BIRCH_WOOD)) birch++;
                else if (s.is(Blocks.ACACIA_LOG) || s.is(Blocks.ACACIA_WOOD)) acacia++;
                else if (s.is(Blocks.DARK_OAK_LOG) || s.is(Blocks.DARK_OAK_WOOD)) darkOak++;
                else if (s.is(Blocks.MANGROVE_LOG) || s.is(Blocks.MANGROVE_WOOD)) mangrove++;
                else if (s.is(Blocks.CHERRY_LOG) || s.is(Blocks.CHERRY_WOOD)) cherry++;
                else if (s.is(Blocks.OAK_LOG) || s.is(Blocks.OAK_WOOD)) oak++;
            }
        }
        int best = Math.max(oak, Math.max(jungle, Math.max(spruce, Math.max(birch, Math.max(acacia, Math.max(darkOak, Math.max(mangrove, cherry)))))));
        if (best == jungle && jungle > 0) return new Palette(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
        if (best == spruce && spruce > 0) return new Palette(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
        if (best == birch && birch > 0) return new Palette(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        if (best == acacia && acacia > 0) return new Palette(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);
        if (best == darkOak && darkOak > 0) return new Palette(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        if (best == mangrove && mangrove > 0) return new Palette(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES);
        if (best == cherry && cherry > 0) return new Palette(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        return new Palette(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
    }

    public record Palette(Block log, Block leaves) {}
}
