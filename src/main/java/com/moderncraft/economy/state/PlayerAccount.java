package com.moderncraft.economy.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player account state. Persisted inside {@link WorldEconomyState} via NBT.
 * <p>
 * Two balance buckets on purpose:
 * <ul>
 *     <li>{@code wallet} — physical cash the player carries.</li>
 *     <li>{@code bank}  — money parked in the bank (not directly spendable).</li>
 * </ul>
 * Splitting them now (instead of "later") keeps the data model stable when we add
 * interest, loans, robbery, and ATM mechanics.
 */
public record PlayerAccount(long wallet, long bank, long totalEarned, long totalSpent) {

    public static final PlayerAccount EMPTY = new PlayerAccount(0L, 0L, 0L, 0L);

    public static final Codec<PlayerAccount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("wallet").forGetter(PlayerAccount::wallet),
            Codec.LONG.fieldOf("bank").forGetter(PlayerAccount::bank),
            Codec.LONG.fieldOf("totalEarned").forGetter(PlayerAccount::totalEarned),
            Codec.LONG.fieldOf("totalSpent").forGetter(PlayerAccount::totalSpent)
    ).apply(instance, PlayerAccount::new));

    public PlayerAccount {
        if (wallet < 0) wallet = 0;
        if (bank < 0) bank = 0;
        if (totalEarned < 0) totalEarned = 0;
        if (totalSpent < 0) totalSpent = 0;
    }

    public PlayerAccount withWallet(long newWallet) {
        return new PlayerAccount(newWallet, bank, totalEarned, totalSpent);
    }

    public PlayerAccount withBank(long newBank) {
        return new PlayerAccount(wallet, newBank, totalEarned, totalSpent);
    }

    public PlayerAccount withEarned(long delta) {
        return new PlayerAccount(wallet, bank, saturatingAdd(totalEarned, delta), totalSpent);
    }

    public PlayerAccount withSpent(long delta) {
        return new PlayerAccount(wallet, bank, totalEarned, saturatingAdd(totalSpent, delta));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
