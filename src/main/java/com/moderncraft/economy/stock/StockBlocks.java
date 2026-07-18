package com.moderncraft.economy.stock;

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

public final class StockBlocks {

    private StockBlocks() {}

    public static final Block STOCK_EXCHANGE = register("stock_exchange",
            new StockExchangeBlock(AbstractBlock.Settings.create()
                    .strength(3.5f).requiresTool()));

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
        Moderncraft.LOGGER.info("[moderncraft] registered 1 stock exchange block");
    }
}
