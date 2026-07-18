package com.moderncraft.economy;

import com.moderncraft.Moderncraft;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Centralised item registration. Each item is a public static final field
 * registered in {@link #register()} on mod init.
 * <p>
 * The mod currently exposes the "phone" — the entry point to the in-game economy UI.
 * Future items (bank card, courier bag, factory parts, stock certificate) get added here.
 */
public final class ModItems {
    private ModItems() {}

    public static final Item PHONE = register(
            "phone",
            new Item.Settings().registryKey(phoneKey()).maxCount(1)
    );

    private static RegistryKey<Item> phoneKey() {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Moderncraft.MOD_ID, "phone"));
    }

    public static void register() {
        Moderncraft.LOGGER.info("[moderncraft] registered {} mod item(s)", 1);
    }

    private static Item register(String name, Item.Settings settings) {
        Identifier id = Identifier.of(Moderncraft.MOD_ID, name);
        Item item = new Item(settings);
        return Registry.register(Registries.ITEM, id, item);
    }
}
