package com.moderncraft.economy.price;

import com.moderncraft.Moderncraft;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Convenience layer that maps vanilla {@link Item}s to {@link ItemPrice}s.
 * Items not present in the catalog return {@link Optional#empty()} — callers
 * can then apply a fallback formula.
 */
public final class PriceCatalogView {

    private PriceCatalogView() {}

    public static Optional<ItemPrice> lookup(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        if (id == null) return Optional.empty();
        ItemPrice p = PriceCatalog.INSTANCE.lookup(id.toString());
        return Optional.ofNullable(p);
    }

    public static Optional<ItemPrice> lookupById(String id) {
        return Optional.ofNullable(PriceCatalog.INSTANCE.lookup(id));
    }

    /** Fallback for unpriced items: 1 M\$ sell / 3 M\$ buy. Visible "we don't know" tier. */
    public static int fallbackSellPrice(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        if (id != null) Moderncraft.LOGGER.debug("No catalog price for {}, using fallback", id);
        return 1;
    }

    public static int fallbackBuyPrice(Item item) {
        return fallbackSellPrice(item) + 2;
    }
}
