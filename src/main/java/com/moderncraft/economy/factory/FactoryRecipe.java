package com.moderncraft.economy.factory;

import net.minecraft.item.Item;

/** A production recipe consumed from the factory's material stock. */
public record FactoryRecipe(Item input, int inputCount, Item output, int minCount, int maxCount, int weight) {
    public FactoryRecipe {
        if (input == null || inputCount <= 0 || output == null || minCount <= 0
                || maxCount < minCount || weight <= 0) {
            throw new IllegalArgumentException("Invalid factory recipe");
        }
    }
}
