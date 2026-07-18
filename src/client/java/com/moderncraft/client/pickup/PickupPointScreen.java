package com.moderncraft.client.pickup;

import com.moderncraft.client.ModerncraftGui;
import com.moderncraft.client.phone.PhoneClientState;
import com.moderncraft.economy.pickup.PickupPointNetworking;
import com.moderncraft.economy.price.PriceCatalogView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Pickup Point screen — lists every priced item in the player's inventory
 * (and a few uncatalogued ones at the bottom as a hint), with a Sell button
 * next to each. There's a "Sell All Priced" button at the top.
 * <p>
 * We don't try to render the full 36-slot grid — only the unique item types
 * with a catalog price, plus a "view full inventory" affordance would be
 * overkill. The screen is intentionally compact and shows the action clearly.
 */
public class PickupPointScreen extends Screen {

    private static final int ROWS_VISIBLE = 8;

    private final List<Entry> entries = new ArrayList<>();
    private int scroll = 0;
    private int tab = 0; // 0 = paid orders, 1 = sell inventory

    public PickupPointScreen() {
        super(Text.literal("Pickup Point"));
    }

    @Override
    protected void init() {
        rebuildEntries();
        rebuildButtons();
    }

    public void refresh() {
        clearChildren();
        init();
    }

    private void rebuildEntries() {
        entries.clear();
        if (tab == 0 || this.client == null || this.client.player == null) return;
        var inv = this.client.player.getInventory();

        // Aggregate by item id so we get one row per item type.
        java.util.Map<String, Integer> byId = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> displayById = new java.util.LinkedHashMap<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id == null) continue;
            String key = id.toString();
            byId.merge(key, stack.getCount(), Integer::sum);
            displayById.putIfAbsent(key, stack.getName().getString());
        }

        // Sort: priced items first (by display name), then unpriced.
        List<String> pricedKeys = new ArrayList<>();
        List<String> unpricedKeys = new ArrayList<>();
        for (String key : byId.keySet()) {
            if (PriceCatalogView.lookupById(key).isPresent()) pricedKeys.add(key);
            else unpricedKeys.add(key);
        }
        java.util.Comparator<String> byName = (a, b) -> displayById.get(a).compareToIgnoreCase(displayById.get(b));
        pricedKeys.sort(byName);
        unpricedKeys.sort(byName);

        for (String key : pricedKeys) {
            int count = byId.get(key);
            int sell = PriceCatalogView.lookupById(key).get().sell();
            int buy = PriceCatalogView.lookupById(key).get().buy();
            entries.add(new Entry(key, displayById.get(key), count, sell, buy, true));
        }
        for (String key : unpricedKeys) {
            int count = byId.get(key);
            entries.add(new Entry(key, displayById.get(key), count, 0, 0, false));
        }
    }

    private void switchTab(int nextTab) {
        tab = nextTab;
        scroll = 0;
        clearChildren();
        init();
    }

    private void rebuildButtons() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Orders"), b -> switchTab(0))
                .dimensions(78, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell"), b -> switchTab(1))
                .dimensions(152, 8, 60, 20).build());
        if (tab == 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Collect all").formatted(Formatting.AQUA),
                    b -> PickupPointNetworking.sendCollect())
                    .dimensions(this.width - 170, 8, 100, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Sell all").formatted(Formatting.GOLD),
                    b -> PickupPointNetworking.sendSellAll())
                    .dimensions(this.width - 170, 8, 100, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(8, this.height - 26, 60, 20).build());

        // Per-row Sell buttons.
        int rowH = 26;
        int top = 40;
        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int idx = i + scroll;
            if (idx >= entries.size()) break;
            Entry e = entries.get(idx);
            int y = top + i * rowH;

            // Sell 1 button.
            if (e.priced) {
                final String key = e.key;
                addDrawableChild(ButtonWidget.builder(
                        Text.literal("Sell 1").formatted(Formatting.GREEN),
                        b -> PickupPointNetworking.sendSell(key, 1))
                        .dimensions(this.width / 2 + 80, y, 60, 18).build());

                // Sell all of this type.
                addDrawableChild(ButtonWidget.builder(
                        Text.literal("Sell " + e.count).formatted(Formatting.AQUA),
                        b -> PickupPointNetworking.sendSell(key, e.count))
                        .dimensions(this.width / 2 + 145, y, 70, 18).build());
            } else {
                addDrawableChild(ButtonWidget.builder(
                        Text.literal("not sold").formatted(Formatting.DARK_GRAY),
                        b -> {})
                        .dimensions(this.width / 2 + 80, y, 135, 18).build());
            }
        }
        // Scroll buttons.
        if (scroll > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
                scroll = Math.max(0, scroll - ROWS_VISIBLE);
                clearChildren();
                init();
            }).dimensions(this.width - 24, top, 18, 18).build());
        }
        int maxScroll = Math.max(0, entries.size() - ROWS_VISIBLE);
        if (scroll < maxScroll) {
            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
                scroll = Math.min(maxScroll, scroll + ROWS_VISIBLE);
                clearChildren();
                init();
            }).dimensions(this.width - 24, top + (ROWS_VISIBLE - 1) * rowH, 18, 18).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(ctx, this.width, this.height);
        ModerncraftGui.header(ctx, this.textRenderer, this.width, tab == 0 ? "Pickup Point · Orders" : "Pickup Point · Sell",
                ModerncraftGui.money(PhoneClientState.wallet));
        if (tab == 0) {
            renderOrders(ctx);
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }
        int orderItems = PhoneClientState.pendingOrders.stream().mapToInt(PhoneClientState.PendingOrder::count).sum();
        String orders = "Paid orders: " + orderItems;
        ctx.drawText(this.textRenderer, orders, 8, 28, 0xFF80C0E0, true);
        // Rows.
        int rowH = 26;
        int top = 40;
        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int idx = i + scroll;
            if (idx >= entries.size()) break;
            Entry e = entries.get(idx);
            int y = top + i * rowH;
            int bg = (i % 2 == 0) ? 0xFF181C22 : 0xFF1A1F26;
            ctx.fill(8, y, this.width / 2 + 70, y + rowH - 2, bg);
            // Item icon (resolved from id).
            ItemStack icon = new ItemStack(PhoneClientState.itemOf(e.key));
            ctx.drawItem(icon, 14, y + 1);
            // Name + count + price.
            Text name = Text.literal(e.name).formatted(Formatting.WHITE);
            if (!e.priced) name = name.formatted(Formatting.DARK_GRAY);
            ctx.drawText(this.textRenderer, name, 50, y + 2, 0xFFFFFFFF, true);
            String sub = "x" + e.count + (e.priced
                    ? "  →  sell " + (e.sell * e.count) + " M$  (" + e.sell + "/u)"
                    : "  (not in catalog)");
            ctx.drawText(this.textRenderer, sub, 50, y + 14,
                    e.priced ? 0xFF80C0E0 : 0xFF606060, true);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderOrders(DrawContext ctx) {
        int total = PhoneClientState.pendingOrders.stream().mapToInt(PhoneClientState.PendingOrder::count).sum();
        ctx.drawText(this.textRenderer, total == 0 ? "No paid orders" : "Paid orders waiting: " + total,
                14, 48, total == 0 ? ModerncraftGui.MUTED : ModerncraftGui.WARNING, true);
        int y = 68;
        for (PhoneClientState.PendingOrder order : PhoneClientState.pendingOrders) {
            if (y > this.height - 54) break;
            ItemStack icon = new ItemStack(PhoneClientState.itemOf(order.itemId()));
            ModerncraftGui.panel(ctx, 12, y, this.width - 24, 34);
            ctx.drawItem(icon, 20, y + 4);
            ctx.drawText(this.textRenderer, icon.getName().getString() + " × " + order.count(),
                    52, y + 6, ModerncraftGui.TEXT, true);
            ctx.drawText(this.textRenderer, "Paid · collect at this Pickup Point",
                    52, y + 19, ModerncraftGui.SELL, false);
            y += 40;
        }
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    private record Entry(String key, String name, int count, int sell, int buy, boolean priced) {}
}
