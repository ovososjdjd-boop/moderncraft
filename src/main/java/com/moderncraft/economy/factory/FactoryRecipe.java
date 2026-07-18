package com.moderncraft.economy.factory;

import net.minecraft.item.Item;

/** A production recipe used by the factory shift simulation. */
public record FactoryRecipe(Item output, int minCount, int maxCount, int weight) {
    public FactoryRecipe {
        if (minCount <= 0 || maxCount < minCount || weight <= 0) {
            throw new IllegalArgumentException("Invalid factory recipe");
        }
    }
}
