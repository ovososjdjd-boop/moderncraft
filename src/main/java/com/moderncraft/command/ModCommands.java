package com.moderncraft.command;

import com.moderncraft.Moderncraft;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Single point of registration for all moderncraft commands.
 * Add new commands by calling more {@code dispatcher.register(...)} here.
 */
public final class ModCommands {

    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BalanceCommands.register(dispatcher);
            CatalogCommands.register(dispatcher);
            VillageCommands.register(dispatcher);
            ModerncraftCommand.register(dispatcher);
            Moderncraft.LOGGER.info("[moderncraft] registered /balance, /catalog, /village, /moderncraft");
        });
    }
}
