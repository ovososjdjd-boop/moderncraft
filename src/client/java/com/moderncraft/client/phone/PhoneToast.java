package com.moderncraft.client.phone;

import com.moderncraft.client.pickup.PickupPointScreen;
import com.moderncraft.economy.phone.PhoneNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shows transient feedback to the player when the server sends a
 * {@link PhoneNetworking.PhoneMessagePayload}. Also handles balance syncs
 * (which the screens read on their own; this just keeps the cache fresh).
 */
public final class PhoneToast {

    private PhoneToast() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PhoneNetworking.PhoneMessagePayload.ID,
                (payload, ctx) -> {
                    MinecraftClient client = ctx.client();
                    client.execute(() -> {
                        Text title = payload.isError()
                                ? Text.literal("Phone — Error").formatted(Formatting.RED)
                                : Text.literal("Phone").formatted(Formatting.GREEN);
                        Text body = Text.literal(payload.text());
                        client.getToastManager().addToast(
                                SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE, title, body));
                        if (client.currentScreen instanceof PickupPointScreen screen && !payload.isError()) {
                            screen.refresh();
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PhoneNetworking.BalanceSyncPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() ->
                            PhoneClientState.onBalanceSync(payload.wallet(), payload.bank()));
                });

        ClientPlayNetworking.registerGlobalReceiver(PhoneNetworking.HistoryPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> PhoneClientState.onHistorySync(payload.encoded())));

        ClientPlayNetworking.registerGlobalReceiver(PhoneNetworking.OpenPhoneAckPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> {
                        PhoneClientState.onBalanceSync(payload.wallet(), payload.bank());
                        MinecraftClient.getInstance().setScreen(new PhoneHomeScreen());
                    });
                });
    }
}
