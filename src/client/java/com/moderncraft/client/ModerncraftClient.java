package com.moderncraft.client;

import com.moderncraft.client.bank.BankToast;
import com.moderncraft.client.factory.FactoryToast;
import com.moderncraft.client.jobs.CourierRenderRegistry;
import com.moderncraft.client.phone.PhoneToast;
import com.moderncraft.client.pickup.PickupPointToast;
import com.moderncraft.client.stock.StockToast;
import com.moderncraft.economy.bank.BankNetworking;
import com.moderncraft.economy.factory.FactoryNetworking;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.pickup.PickupPointNetworking;
import com.moderncraft.economy.stock.StockNetworking;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entry point.
 */
public final class ModerncraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Network payload types (both sides).
        PhoneNetworking.registerCommon();
        PickupPointNetworking.registerCommon();
        FactoryNetworking.registerCommon();
        BankNetworking.registerCommon();
        StockNetworking.registerCommon();

        // Screen / toast handlers.
        PhoneToast.register();
        PickupPointToast.register();
        FactoryToast.register();
        BankToast.register();
        StockToast.register();

        // Entity renderers.
        CourierRenderRegistry.register();
    }
}
