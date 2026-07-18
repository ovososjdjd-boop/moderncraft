package com.moderncraft.economy;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.pickup.PickupPointBlock;
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
 * Centralised block registration. The real buildings (Pickup Point, Factory,
 * Cafe, Bank, Loader Depot) get added here as we build them.
 */
public final class ModBlocks {
    private ModBlocks() {}

    // The Pickup Point — where players sell items. Counterpart to the phone's catalog.
    public static final Block PICKUP_POINT = registerWithItem(
            "pickup_point",
            new PickupPointBlock(AbstractBlock.Settings.create()
                    .strength(3.0f)
                    .requiresTool())
    );

    private static Block registerWithItem(String name, Block block) {
        Identifier id = Identifier.of(Moderncraft.MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);

        Block registered = Registry.register(Registries.BLOCK, blockKey, block);
        BlockItem item = new BlockItem(registered, new Item.Settings().registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return registered;
    }

    public static void register() {
        Moderncraft.LOGGER.info("[moderncraft] registered {} mod block(s)", 1);
    }
}
