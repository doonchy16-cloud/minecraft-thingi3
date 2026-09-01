package dev.worldmind.observe;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import dev.worldmind.content.WorldmindBlocks;
import dev.worldmind.state.PlaceKind;

public final class PlaceRecognizer {
    private static final int HORIZONTAL = 6;
    private static final int VERTICAL = 4;

    public Optional<ObservedPlace> recognize(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos anchor = player.blockPosition();
        int beds = 0, containers = 0, work = 0, doors = 0, farms = 0, solid = 0;
        int dirt = 0, wood = 0, stone = 0, defensive = 0;
        boolean sealed = false;

        for (int dx = -HORIZONTAL; dx <= HORIZONTAL; dx++) {
            for (int dz = -HORIZONTAL; dz <= HORIZONTAL; dz++) {
                for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    solid++;
                    if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK)) dirt++;
                    if (state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS) || state.is(Blocks.BIRCH_PLANKS)
                            || state.is(Blocks.DARK_OAK_PLANKS) || state.is(Blocks.COBBLESTONE)) wood += state.is(Blocks.COBBLESTONE) ? 0 : 1;
                    if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS)) stone++;
                    if (state.is(Blocks.COBBLESTONE_WALL) || state.is(Blocks.IRON_BARS)) defensive++;
                    if (state.is(BlockTags.BEDS)) beds++;
                    if (level.getBlockEntity(pos) != null) containers++;
                    if (state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                            || state.is(Blocks.SMOKER) || state.is(Blocks.ANVIL)) work++;
                    if (state.getBlock() instanceof DoorBlock) doors++;
                    if (state.is(Blocks.FARMLAND)) farms++;
                    if (state.is(WorldmindBlocks.WORLDSEAL)) sealed = true;
                }
            }
        }

        double confidence = Math.min(1.0,
                beds * 0.34 + Math.min(containers, 3) * 0.12 + Math.min(work, 3) * 0.10
                        + Math.min(doors, 2) * 0.07 + Math.min(farms, 12) * 0.01 + (solid >= 50 ? 0.12 : 0.0));
        if (confidence < 0.58) return Optional.empty();

        PlaceKind kind = farms >= 8 && beds >= 1 ? PlaceKind.FARMSTEAD
                : beds >= 1 && (containers + work) >= 2 ? PlaceKind.HOMESTEAD
                : PlaceKind.OUTPOST;
        String palette = dirt >= wood && dirt >= stone ? "dirt" : wood >= stone ? "wood" : "stone";
        double defensiveIntent = Math.min(1.0, defensive * 0.12 + (doors >= 2 ? 0.12 : 0));
        double unfinishedIntent = Math.min(1.0, (solid < 90 ? 0.45 : 0.15) + (doors == 0 ? 0.15 : 0));
        double expansionIntent = Math.min(1.0, farms * 0.02 + work * 0.08 + containers * 0.04);
        StructureIntentProfile structure = new StructureIntentProfile(kind.name().toLowerCase(), palette,
                defensiveIntent, unfinishedIntent, expansionIntent, confidence);
        return Optional.of(new ObservedPlace(anchor.getX(), anchor.getY(), anchor.getZ(), 10, confidence, kind, sealed, structure));
    }
}
