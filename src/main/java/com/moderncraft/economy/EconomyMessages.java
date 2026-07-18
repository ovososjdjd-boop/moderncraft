package com.moderncraft.economy;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Centralised chat / HUD text for the economy so we can re-skin without hunting through logic.
 */
public final class EconomyMessages {

    private EconomyMessages() {}

    public static Text welcome(long starterAmount) {
        return Text.empty()
                .append(Text.literal("Welcome to Moderncraft! ").formatted(Formatting.GOLD))
                .append(Text.literal("You received " + starterAmount + " M$ to get started. ").formatted(Formatting.GREEN))
                .append(Text.literal("Type /balance to see your money.").formatted(Formatting.GRAY));
    }
}
