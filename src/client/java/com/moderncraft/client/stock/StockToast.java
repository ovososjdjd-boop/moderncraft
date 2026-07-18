package com.moderncraft.client.stock;

import com.moderncraft.economy.stock.StockNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class StockToast {

    private StockToast() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(StockNetworking.OpenScreenPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> {
                        StockScreen screen = new StockScreen();
                        screen.setSnapshot(
                                payload.companyIds(),
                                payload.companyNames(),
                                payload.companyPrices(),
                                payload.shareCounts(),
                                payload.wallet()
                        );
                        MinecraftClient.getInstance().setScreen(screen);
                    });
                });
    }
}
