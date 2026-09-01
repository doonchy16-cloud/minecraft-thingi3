package dev.worldmind.content;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import dev.worldmind.WorldmindMod;

public final class WorldmindBlocks {
    public static final BlockItemId WORLDSEAL_ID = BlockItemId.create(
            Identifier.fromNamespaceAndPath(WorldmindMod.MOD_ID, "worldseal"),
            Identifier.fromNamespaceAndPath(WorldmindMod.MOD_ID, "worldseal"));

    public static final Block WORLDSEAL = register(
            WORLDSEAL_ID,
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE));

    private WorldmindBlocks() {}

    private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(id.block()));
        Registry.register(BuiltInRegistries.BLOCK, id.block(), block);
        BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), item);
        return block;
    }

    public static void initialize() {
        // Static initialization performs registration.
    }
}
