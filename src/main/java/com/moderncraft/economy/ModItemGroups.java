package com.moderncraft.economy;

import com.moderncraft.Moderncraft;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Custom creative tab so players can find modded items.
 */
public final class ModItemGroups {
    private ModItemGroups() {}

    private static final RegistryKey<ItemGroup> MOD_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(Moderncraft.MOD_ID, "main")
    );

    private static final ItemGroup MOD_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.PHONE))
            .displayName(Text.translatable("itemGroup.moderncraft.main"))
            .entries((ctx, entries) -> {
                entries.add(ModItems.PHONE);
                entries.add(ModBlocks.PICKUP_POINT);
                entries.add(com.moderncraft.economy.factory.FactoryBlocks.BASE);
                entries.add(com.moderncraft.economy.factory.FactoryBlocks.CHIMNEY);
                entries.add(com.moderncraft.economy.factory.FactoryBlocks.PIPE);
                entries.add(com.moderncraft.economy.factory.FactoryBlocks.GEAR);
                entries.add(com.moderncraft.economy.factory.FactoryBlocks.NOTICE_BOARD);
                entries.add(com.moderncraft.economy.bank.BankBlocks.BANK);
                entries.add(com.moderncraft.economy.stock.StockBlocks.STOCK_EXCHANGE);
                entries.add(com.moderncraft.economy.jobs.JobBlocks.CAFE_BOARD);
                entries.add(com.moderncraft.economy.jobs.JobBlocks.LOADER_BOARD);
                entries.add(com.moderncraft.economy.jobs.JobBlocks.LOADER_TARGET);
                entries.add(com.moderncraft.economy.jobs.CourierEntities.CAFE_COURIER_SPAWN_EGG);
                entries.add(com.moderncraft.economy.jobs.LoaderEntities.LOADER_SPAWN_EGG);
            })
            .build();

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, MOD_GROUP_KEY, MOD_GROUP);
        Moderncraft.LOGGER.info("[moderncraft] registered creative item group");
    }
}
