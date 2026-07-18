package com.moderncraft.client.phone;

import com.moderncraft.economy.price.ItemPrice;
import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.price.PriceCategory;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side mirror of the player's economy state. Updated by network
 * packets; consumed by screens. Pure data holder, no behaviour.
 * <p>
 * We don't need to be authoritative on the client — the server is — but we
 * mirror the last-known balances so the UI can show them instantly without a
 * round-trip on every paint.
 */
public final class PhoneClientState {

    public static long wallet = 0L;
    public static long bank = 0L;
    public static final List<PendingOrder> pendingOrders = new ArrayList<>();
    public static final List<HistoryEntry> history = new ArrayList<>();

    public record PendingOrder(String itemId, int count) {}
    public record HistoryEntry(String type, long amount, long timestamp, String note) {}

    public static void onHistorySync(String encoded) {
        history.clear();
        if (encoded == null || encoded.isBlank()) return;
        for (String row : encoded.split(";", -1)) {
            String[] parts = row.split("\\|", 4);
            if (parts.length == 4) {
                try { history.add(new HistoryEntry(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), parts[3])); }
                catch (NumberFormatException ignored) { }
            }
        }
    }

    public static void onOrdersSync(String encoded) {
        pendingOrders.clear();
        if (encoded == null || encoded.isBlank()) return;
        for (String row : encoded.split(";")) {
            String[] parts = row.split("\\|", 2);
            if (parts.length == 2) {
                try { pendingOrders.add(new PendingOrder(parts[0], Integer.parseInt(parts[1]))); }
                catch (NumberFormatException ignored) { }
            }
        }
    }

    private PhoneClientState() {}

    public static void onBalanceSync(long newWallet, long newBank) {
        wallet = newWallet;
        bank = newBank;
    }

    // --- catalog helpers (client-side, no network) --------------------------

    public static List<PriceCategory> categories() {
        return PriceCatalog.INSTANCE.categories();
    }

    public static List<ItemPrice> itemsInCategory(String id) {
        return PriceCatalog.INSTANCE.byCategory(id);
    }

    public static Item itemOf(String namespacedId) {
        Identifier ident = Identifier.tryParse(namespacedId);
        if (ident == null) ident = Identifier.of("minecraft", namespacedId);
        return Registries.ITEM.get(ident);
    }

    /** Convenience: returns all items flattened, used by the search-as-you-type bar. */
    public static List<ItemPrice> search(String query) {
        if (query == null || query.isBlank()) {
            List<ItemPrice> all = new ArrayList<>();
            for (PriceCategory c : categories()) all.addAll(itemsInCategory(c.id()));
            return all;
        }
        String q = query.toLowerCase().trim();
        List<ItemPrice> hits = new ArrayList<>();
        for (PriceCategory c : categories()) {
            for (ItemPrice p : itemsInCategory(c.id())) {
                if (p.displayName().toLowerCase().contains(q) || p.id().contains(q)) {
                    hits.add(p);
                }
            }
        }
        return hits;
    }
}
