package com.moderncraft.client.bank;

import com.moderncraft.economy.bank.BankNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class BankToast {

    private BankToast() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BankNetworking.OpenScreenPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        BankScreen screen = new BankScreen(payload.pos());
                        screen.setBalances(payload.wallet(), payload.bank(), payload.branchTotal());
                        mc.setScreen(screen);
                    });
                });
    }
}
