package com.moderncraft.economy.factory;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * The "Factory" — a multi-block building where players work shifts to
 * produce redstone parts and earn M\$.
 * <p>
 * The factory is a <b>structural</b> multi-block: the {@link FactoryBaseBlock}
 * is the only block with a tile entity; the others are decorative but they
 * must be in the right relative positions for the building to be considered
 * "complete" (and for the base to actually operate). This keeps the logic
 * simple — we don't need to model the whole structure as a single block
 * entity.
 */
public final class FactoryBlocks {

    private FactoryBlocks() {}

    /** The main block — has the BlockEntity, opens the GUI. */
    public static final Block BASE = register("factory_base",
            new FactoryBaseBlock(AbstractBlock.Settings.create()
                    .strength(4.0f).requiresTool().nonOpaque()));

    /** Decorative chimney that must be 1-2 blocks above the base. */
    public static final Block CHIMNEY = register("factory_chimney",
            new Block(AbstractBlock.Settings.create()
                    .strength(3.5f).requiresTool()));

    /** Decorative pipe, must be on the side of the base. */
    public static final Block PIPE = register("factory_pipe",
            new Block(AbstractBlock.Settings.create()
                    .strength(3.5f).requiresTool()));

    /** Decorative gear on top of the base, rotates while operating. */
    public static final Block GEAR = register("factory_gear",
            new FactoryGearBlock(AbstractBlock.Settings.create()
                    .strength(3.5f).requiresTool().nonOpaque()));

    /** The job-issuer block — like an NPC, but a decorative notice board. */
    public static final Block NOTICE_BOARD = register("factory_notice_board",
            new FactoryNoticeBoardBlock(AbstractBlock.Settings.create()
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
        Moderncraft.LOGGER.info("[moderncraft] registered 4 factory blocks");
    }
}
