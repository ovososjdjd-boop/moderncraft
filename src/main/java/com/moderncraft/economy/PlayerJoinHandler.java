package com.moderncraft.economy;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.state.EconomyService;
import com.moderncraft.economy.state.PlayerAccount;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hooks into the server-side join lifecycle:
 * <ul>
 *     <li>On first join we grant a small starter wallet so the economy is playable
 *         from the very first tick (without this, {@code /balance} shows 0 0 forever
 *         and players can't even test the {@code pay} command).</li>
 *     <li>Future hooks (e.g. onDisconnect analytics, daily login bonus) live here.</li>
 * </ul>
 */
public final class PlayerJoinHandler {

    /** Money every player starts with. Enough to feel the loop, small enough to still need to work. */
    private static final long STARTER_WALLET = 250L;

    private PlayerJoinHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PlayerAccount account = EconomyService.view(server, player.getUuid());
            // First-join detection: if totalEarned is 0 AND wallet is 0 AND bank is 0.
            if (account.totalEarned() == 0 && account.wallet() == 0 && account.bank() == 0) {
                EconomyService.creditWallet(server, player.getUuid(), STARTER_WALLET);
                Moderncraft.LOGGER.info("Granted starter wallet ({}) to new player {}",
                        STARTER_WALLET, player.getName().getString());
                // Defer the welcome message slightly — the player isn't actually
                // fully in the world yet at the JOIN event.
                server.execute(() -> {
                    if (player.isConnected()) {
                        player.sendMessage(
                                com.moderncraft.economy.EconomyMessages.welcome(STARTER_WALLET),
                                false);
                    }
                });
            }
        });
    }
}
