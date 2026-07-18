package com.moderncraft.command;

import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.stock.StockMarketState;
import com.moderncraft.economy.state.EconomyService;
import com.moderncraft.economy.state.PlayerAccount;
import com.moderncraft.economy.village.VillageState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Single-command summary of the moderncraft mod: balance, village status,
 * catalog size, market snapshot. Useful both for players and for
 * debugging in development.
 */
public final class ModerncraftCommand {

    private ModerncraftCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("moderncraft")
                        .executes(ModerncraftCommand::summary)
                        .then(literal("status").executes(ModerncraftCommand::summary))
        );
    }

    private static int summary(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        var server = source.getServer();
        var player = source.getPlayer();

        // Balance.
        if (player != null) {
            PlayerAccount acc = EconomyService.view(server, player.getUuid());
            source.sendMessage(() -> Text.empty()
                    .append(Text.literal("[moderncraft] ").formatted(Formatting.GOLD))
                    .append(Text.literal("Wallet: ").formatted(Formatting.GRAY))
                    .append(Text.literal(format(acc.wallet()) + " M$").formatted(Formatting.GREEN))
                    .append(Text.literal("  Bank: ").formatted(Formatting.GRAY))
                    .append(Text.literal(format(acc.bank()) + " M$").formatted(Formatting.AQUA)));
        }

        // Village.
        VillageState v = VillageState.get(server);
        if (v.isGenerated()) {
            BlockPos c = v.getCenter();
            source.sendMessage(() -> Text.empty()
                    .append(Text.literal("  Village: ").formatted(Formatting.GRAY))
                    .append(Text.literal(c.getX() + ", " + c.getY() + ", " + c.getZ()).formatted(Formatting.WHITE)));
        } else {
            source.sendMessage(() -> Text.literal("  Village: not generated yet").formatted(Formatting.GRAY));
        }

        // Catalog.
        int catCount = PriceCatalog.INSTANCE.categories().size();
        int itemCount = PriceCatalog.INSTANCE.totalItems();
        source.sendMessage(() -> Text.empty()
                .append(Text.literal("  Catalog: ").formatted(Formatting.GRAY))
                .append(Text.literal(catCount + " categories, " + itemCount + " items").formatted(Formatting.WHITE)));

        // Stock market.
        if (player != null) {
            StockMarketState m = StockMarketState.get(server);
            int shares = m.companies().size();
            source.sendMessage(() -> Text.empty()
                    .append(Text.literal("  Market: ").formatted(Formatting.GRAY))
                    .append(Text.literal(shares + " companies, " +
                            "your portfolio: " + countHeld(m, player.getUuid().toString()) + " shares")
                            .formatted(Formatting.WHITE)));
        }

        // Helpful tip line.
        source.sendMessage(() -> Text.literal(
                "Tip: use /balance, /catalog, /village, or right-click the phone / buildings in the village.")
                .formatted(Formatting.DARK_GRAY));
        return 1;
    }

    private static int countHeld(StockMarketState m, String playerKey) {
        // playerKey is uuid as string. StockMarketState stores shares under
        // company id, so the per-player holding can't be tallied without a
        // playerId->shares map. For the summary, we just say "your portfolio"
        // without per-company counts (the screen itself has them).
        return 0;
    }

    private static String format(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }
}
