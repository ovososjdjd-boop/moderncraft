package com.moderncraft.economy.price;

/**
 * A single priced entry. The id is a vanilla Minecraft namespaced id, e.g.
 * {@code minecraft:diamond} or {@code minecraft:oak_planks}. The namespace
 * defaults to {@code minecraft} if omitted in the JSON.
 * <p>
 * The price model is intentionally simple: one {@code buy} (player pays) and one
 * {@code sell} (player receives). {@code sell} is always &lt; {@code buy} so
 * the economy never prints money. {@code category} is duplicated from the
 * per-category file so debug / overlay screens can show it without an extra
 * lookup.
 */
public record ItemPrice(String id, String category, int buy, int sell, String displayName) {

    public ItemPrice {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ItemPrice.id must be non-blank");
        }
        if (buy < 0) buy = 0;
        if (sell < 0) sell = 0;
    }

    /** Constructor used by JSON loading — no display name yet. */
    public ItemPrice(String id, String category, int buy, int sell) {
        this(id, category, buy, sell, humanise(id));
    }

    /** Splits "minecraft:diamond" -> "diamond"; turns "ender_pearl" into "Ender Pearl". */
    private static String humanise(String namespacedId) {
        String path = namespacedId.contains(":") ? namespacedId.split(":", 2)[1] : namespacedId;
        StringBuilder out = new StringBuilder(path.length());
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_') {
                out.append(' ');
                upper = true;
            } else if (upper) {
                out.append(Character.toUpperCase(c));
                upper = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
