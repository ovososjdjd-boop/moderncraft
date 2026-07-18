package com.moderncraft.client.factory;

import com.moderncraft.economy.factory.FactoryNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Client handler for factory packets: opens the screen, updates the local
 * state mirror, refreshes the screen if it's already open.
 */
public final class FactoryToast {

    private FactoryToast() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FactoryNetworking.OpenScreenPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() ->
                            MinecraftClient.getInstance().setScreen(new FactoryScreen(payload.pos())));
                });

        ClientPlayNetworking.registerGlobalReceiver(FactoryNetworking.FactoryStatePayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> {
                        FactoryScreen.FactoryState s = new FactoryScreen.FactoryState(
                                payload.pos(),
                                payload.structureValid(),
                                payload.hasOwner(),
                                payload.isYouOwner(),
                                payload.shiftTicks(),
                                payload.shiftDuration(),
                                payload.producedIds(),
                                payload.producedCounts()
                        );
                        FactoryScreen.onState(s);
                        // If the screen is currently open, rebuild the buttons
                        // so the Start/End shift button reflects the new state.
                        if (MinecraftClient.getInstance().currentScreen instanceof FactoryScreen fs) {
                            fs.refresh();
                        }
                    });
                });
    }
}
