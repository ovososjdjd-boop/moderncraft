package com.moderncraft.economy.jobs;

import com.moderncraft.Moderncraft;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class JobBlocks {

    private JobBlocks() {}

    public static final Block CAFE_BOARD = register("cafe_board",
            new JobBoardBlock("cafe", AbstractBlock.Settings.create()
                    .strength(2.5f).requiresTool().nonOpaque()));

    public static final Block LOADER_BOARD = register("loader_board",
            new JobBoardBlock("loader", AbstractBlock.Settings.create()
                    .strength(2.5f).requiresTool().nonOpaque()));

    public static final Block LOADER_TARGET = register("loader_target",
            new LoaderTargetBlock(AbstractBlock.Settings.create()
                    .strength(2.5f).requiresTool().nonOpaque()));

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of(Moderncraft.MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Block registered = Registry.register(Registries.BLOCK, blockKey, block);
        BlockItem item = new BlockItem(registered, new Item.Settings().registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return registered;
    }

    public static void register() {
        Moderncraft.LOGGER.info("[moderncraft] registered 3 job-board blocks");
    }
}
