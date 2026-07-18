package com.moderncraft.economy.price;

import java.util.List;

/**
 * Metadata for one category (e.g. "ores", "food"). The actual prices live in
 * the corresponding per-category JSON; this record only describes the category
 * itself for the in-game UI.
 */
public record PriceCategory(
        String id,
        String displayName,
        int order,
        String icon,
        List<String> fileNames
) {
}
