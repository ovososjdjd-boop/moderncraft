package com.moderncraft.economy.state;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * High-level API on top of {@link WorldEconomyState}. All gameplay code that needs
 * to touch money goes through here — never directly through the persistent state.
 * <p>
 * This is where we put rules: minimum amounts, overdraft policy, transfer limits.
 * Right now it's a thin pass-through, but it's the right place to grow.
 */
public final class EconomyService {

    private EconomyService() {}

    public enum Result {
        OK,
        NOT_ENOUGH_WALLET,
        NOT_ENOUGH_BANK,
        PLAYER_NOT_FOUND,
        INVALID_AMOUNT
    }

    /** Read-only snapshot. */
    public static PlayerAccount view(MinecraftServer server, UUID playerId) {
        return WorldEconomyState.get(server).getOrCreate(playerId);
    }

    /** Add money directly to the wallet. Used by jobs, sell actions, /balance give. */
    public static Result creditWallet(MinecraftServer server, UUID playerId, long amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        WorldEconomyState state = WorldEconomyState.get(server);
        PlayerAccount current = state.getOrCreate(playerId);
        state.put(playerId, current.withWallet(saturatingAdd(current.wallet(), amount))
                .withEarned(amount));
        record(server, playerId, "credit", amount, "Income");
        return Result.OK;
    }

    /** Take money from the wallet. Returns NOT_ENOUGH_WALLET if balance would go negative. */
    public static Result debitWallet(MinecraftServer server, UUID playerId, long amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        WorldEconomyState state = WorldEconomyState.get(server);
        PlayerAccount current = state.getOrCreate(playerId);
        if (current.wallet() < amount) return Result.NOT_ENOUGH_WALLET;
        state.put(playerId, current.withWallet(current.wallet() - amount).withSpent(amount));
        record(server, playerId, "debit", amount, "Payment");
        return Result.OK;
    }

    public static Result depositToBank(MinecraftServer server, UUID playerId, long amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        WorldEconomyState state = WorldEconomyState.get(server);
        PlayerAccount current = state.getOrCreate(playerId);
        if (current.wallet() < amount) return Result.NOT_ENOUGH_WALLET;
        if (current.bank() > Long.MAX_VALUE - amount) return Result.INVALID_AMOUNT;
        state.put(playerId, current
                .withWallet(current.wallet() - amount)
                .withBank(current.bank() + amount));
        record(server, playerId, "deposit", amount, "Wallet to bank");
        return Result.OK;
    }

    public static Result withdrawFromBank(MinecraftServer server, UUID playerId, long amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        WorldEconomyState state = WorldEconomyState.get(server);
        PlayerAccount current = state.getOrCreate(playerId);
        if (current.bank() < amount) return Result.NOT_ENOUGH_BANK;
        if (current.wallet() > Long.MAX_VALUE - amount) return Result.INVALID_AMOUNT;
        state.put(playerId, current
                .withBank(current.bank() - amount)
                .withWallet(current.wallet() + amount));
        record(server, playerId, "withdraw", amount, "Bank to wallet");
        return Result.OK;
    }

    /**
     * Move money directly from one wallet to another. Atomic on a single server
     * because we read both players, check, then write both — Minecraft is single-threaded
     * for command execution, so this is safe.
     */
    public static Result transferWallet(MinecraftServer server, UUID from, UUID to, long amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        if (from.equals(to)) return Result.INVALID_AMOUNT;
        WorldEconomyState state = WorldEconomyState.get(server);
        PlayerAccount sender = state.getOrCreate(from);
        if (sender.wallet() < amount) return Result.NOT_ENOUGH_WALLET;
        PlayerAccount receiver = state.getOrCreate(to);
        if (receiver.wallet() > Long.MAX_VALUE - amount) return Result.INVALID_AMOUNT;
        state.put(from, sender.withWallet(sender.wallet() - amount).withSpent(amount));
        state.put(to, receiver.withWallet(receiver.wallet() + amount).withEarned(amount));
        record(server, from, "transfer_out", amount, "Player transfer");
        record(server, to, "transfer_in", amount, "Player transfer");
        return Result.OK;
    }

    public static void recordEvent(MinecraftServer server, UUID playerId, String type, long amount, String note) {
        record(server, playerId, type, amount, note);
    }

    private static void record(MinecraftServer server, UUID playerId, String type, long amount, String note) {
        long time = server.getOverworld() == null ? 0L : server.getOverworld().getTime();
        WorldEconomyState.get(server).appendLedger(playerId, new EconomyLedgerEntry(type, amount, time, note));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
