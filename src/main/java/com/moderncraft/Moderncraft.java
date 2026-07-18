package com.moderncraft;

import com.moderncraft.command.ModCommands;
import com.moderncraft.economy.ModBlockEntities;
import com.moderncraft.economy.ModBlocks;
import com.moderncraft.economy.ModItemGroups;
import com.moderncraft.economy.ModItems;
import com.moderncraft.economy.PlayerJoinHandler;
import com.moderncraft.economy.bank.BankBlocks;
import com.moderncraft.economy.bank.BankNetworking;
import com.moderncraft.economy.factory.FactoryBlocks;
import com.moderncraft.economy.factory.FactoryNetworking;
import com.moderncraft.economy.jobs.CourierEntities;
import com.moderncraft.economy.jobs.CourierJobHandler;
import com.moderncraft.economy.jobs.JobBlocks;
import com.moderncraft.economy.jobs.LoaderEntities;
import com.moderncraft.economy.phone.PhoneItems;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.pickup.PickupPointNetworking;
import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.stock.StockBlocks;
import com.moderncraft.economy.stock.StockNetworking;
import com.moderncraft.economy.village.VillageJoinHook;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Moderncraft on the logical server (and singleplayer).
 */
public final class Moderncraft implements ModInitializer {
    public static final String MOD_ID = "moderncraft";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[moderncraft] booting core subsystems...");

        ModItems.register();
        ModBlocks.register();
        FactoryBlocks.register();
        BankBlocks.register();
        StockBlocks.register();
        JobBlocks.register();
        CourierEntities.register();
        LoaderEntities.register();
        ModBlockEntities.register();
        ModItemGroups.register();

        // Economy: commands, server-side join handler, and the price catalog listener.
        ModCommands.register();
        PlayerJoinHandler.register();
        VillageJoinHook.register();
        CourierJobHandler.register();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                PriceCatalog.newReloadListener());

        // Network: phone, pickup point, factory, bank, stock.
        PhoneNetworking.registerCommon();
        PhoneNetworking.registerServer();
        PickupPointNetworking.registerCommon();
        PickupPointNetworking.registerServer();
        FactoryNetworking.registerCommon();
        FactoryNetworking.registerServer();
        BankNetworking.registerCommon();
        BankNetworking.registerServer();
        StockNetworking.registerCommon();
        StockNetworking.registerServer();
        PhoneItems.register();

        LOGGER.info("[moderncraft] ready. Full village, all buildings, economy, catalog online.");
    }
}
