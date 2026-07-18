package com.moderncraft.command;

import com.moderncraft.economy.state.EconomyService;
import com.moderncraft.economy.state.PlayerAccount;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers {@code /balance} and subcommands. Lives on the dedicated server
 * (and integrated server for singleplayer). We never touch money on the client.
 */
public final class BalanceCommands {

    private BalanceCommands() {}

    /** Suggests names of currently online players. */
    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS =
            (context, builder) -> {
                MinecraftServer server = context.getSource().getServer();
                String remaining = builder.getRemainingLowerCase();
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    if (p.getName().getString().toLowerCase().startsWith(remaining)) {
                        builder.suggest(p.getName().getString());
                    }
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("balance")
                        .executes(BalanceCommands::runSelf)
                        .then(literal("give")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(argument("player", StringArgumentType.string())
                                        .suggests(ONLINE_PLAYERS)
                                        .then(argument("amount", LongArgumentType.longArg(1))
                                                .executes(BalanceCommands::runGive))))
                        .then(literal("pay")
                                .then(argument("player", StringArgumentType.string())
                                        .suggests(ONLINE_PLAYERS)
                                        .then(argument("amount", LongArgumentType.longArg(1))
                                                .executes(BalanceCommands::runPay))))
                        .then(literal("deposit")
                                .then(argument("amount", LongArgumentType.longArg(1))
                                        .executes(BalanceCommands::runDeposit)))
                        .then(literal("withdraw")
                                .then(argument("amount", LongArgumentType.longArg(1))
                                        .executes(BalanceCommands::runWithdraw)))
        );
    }

    // --- handlers -----------------------------------------------------------

    private static int runSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command must be run as a player."));
            return 0;
        }
        PlayerAccount acc = EconomyService.view(ctx.getSource().getServer(), player.getUuid());
        ctx.getSource().sendMessage(() -> Text.empty()
                .append(Text.literal("Wallet: ").formatted(Formatting.GRAY))
                .append(Text.literal(format(acc.wallet()) + " M$").formatted(Formatting.GREEN))
                .append(Text.literal("   Bank: ").formatted(Formatting.GRAY))
                .append(Text.literal(format(acc.bank()) + " M$").formatted(Formatting.AQUA))
        );
        return 1;
    }

    private static int runGive(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = resolveTarget(ctx);
        if (target == null) return 0;
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyService.Result r = EconomyService.creditWallet(
                ctx.getSource().getServer(), target.getUuid(), amount);
        if (r == EconomyService.Result.OK) {
            ctx.getSource().sendMessage(Text.literal(
                    "Gave " + amount + " M$ to " + target.getName().getString() + ".").formatted(Formatting.GREEN));
            target.sendMessage(Text.literal("Received " + amount + " M$ (admin grant).").formatted(Formatting.GREEN));
        }
        return reportResult(ctx, r);
    }

    private static int runPay(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity sender = ctx.getSource().getPlayer();
        if (sender == null) {
            ctx.getSource().sendError(Text.literal("Only players can pay."));
            return 0;
        }
        ServerPlayerEntity target = resolveTarget(ctx);
        if (target == null) return 0;
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyService.Result r = EconomyService.transferWallet(
                ctx.getSource().getServer(), sender.getUuid(), target.getUuid(), amount);
        if (r == EconomyService.Result.OK) {
            sender.sendMessage(Text.literal("Sent " + amount + " M$ to " + target.getName().getString() + ".").formatted(Formatting.GREEN));
            target.sendMessage(Text.literal("Received " + amount + " M$ from " + sender.getName().getString() + ".").formatted(Formatting.GREEN));
        }
        return reportResult(ctx, r);
    }

    private static int runDeposit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Only players can deposit."));
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyService.Result r = EconomyService.depositToBank(ctx.getSource().getServer(), player.getUuid(), amount);
        if (r == EconomyService.Result.OK) {
            player.sendMessage(Text.literal("Deposited " + amount + " M$ into the bank.").formatted(Formatting.GREEN));
        }
        return reportResult(ctx, r);
    }

    private static int runWithdraw(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Only players can withdraw."));
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyService.Result r = EconomyService.withdrawFromBank(ctx.getSource().getServer(), player.getUuid(), amount);
        if (r == EconomyService.Result.OK) {
            player.sendMessage(Text.literal("Withdrew " + amount + " M$ from the bank.").formatted(Formatting.GREEN));
        }
        return reportResult(ctx, r);
    }

    // --- helpers ------------------------------------------------------------

    /** Resolves the "player" string argument into an online ServerPlayerEntity, or reports and returns null. */
    private static ServerPlayerEntity resolveTarget(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(name);
        if (target == null) {
            ctx.getSource().sendError(Text.literal("Player '" + name + "' is not online.").formatted(Formatting.RED));
            return null;
        }
        return target;
    }

    private static int reportResult(CommandContext<ServerCommandSource> ctx, EconomyService.Result r) {
        switch (r) {
            case NOT_ENOUGH_WALLET ->
                    ctx.getSource().sendError(Text.literal("Not enough money in wallet.").formatted(Formatting.RED));
            case NOT_ENOUGH_BANK ->
                    ctx.getSource().sendError(Text.literal("Not enough money in bank.").formatted(Formatting.RED));
            case PLAYER_NOT_FOUND ->
                    ctx.getSource().sendError(Text.literal("Player not found.").formatted(Formatting.RED));
            case INVALID_AMOUNT ->
                    ctx.getSource().sendError(Text.literal("Amount must be positive.").formatted(Formatting.RED));
            case OK -> { return 1; }
        }
        return 0;
    }

    /** Formats large numbers with spaces: 1 234 567 instead of 1234567. */
    private static String format(long value) {
        return String.format("%,d", value).replace(',', ' ');
    }
}
