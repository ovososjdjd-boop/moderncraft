package com.moderncraft.client.pickup;

import com.moderncraft.economy.pickup.PickupPointNetworking;
import com.moderncraft.client.phone.PhoneClientState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Client handler for the Pickup Point open payload — opens the screen.
 * Sell/balance updates go through the existing phone toast / phone client state.
 */
public final class PickupPointToast {

    private PickupPointToast() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PickupPointNetworking.OpenScreenPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() ->
                            MinecraftClient.getInstance().setScreen(new PickupPointScreen()));
                });
        ClientPlayNetworking.registerGlobalReceiver(PickupPointNetworking.PendingOrdersPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    PhoneClientState.onOrdersSync(payload.encoded());
                    if (MinecraftClient.getInstance().currentScreen instanceof PickupPointScreen screen) {
                        screen.refresh();
                    }
                }));
    }
}
