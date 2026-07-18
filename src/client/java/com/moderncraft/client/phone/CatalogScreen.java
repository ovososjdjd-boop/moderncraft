package com.moderncraft.client.phone;

import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.price.ItemPrice;
import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.price.PriceCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Browse the catalog by category, see prices, and buy 1 of any item.
 * <p>
 * Layout: tab strip on the left (one button per category), item grid on the
 * right (5 columns of icon tiles). Click a tile to buy 1.
 */
public class CatalogScreen extends Screen {

    private static final int TILE = 36;
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int GAP = 4;
    private static final int TAB_W = 88;

    private String currentCategoryId = "ores";
    private int scroll = 0;

    public CatalogScreen() {
        super(Text.literal("Catalog"));
    }

    @Override
    protected void init() {
        rebuildTabs();
        rebuildGrid();
        // Back button.
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(8, this.height - 26, 60, 20)
                .build());
    }

    private void rebuildTabs() {
        // Tabs are on the left edge, scrollable if they don't fit. We rebuild
        // from scratch every category change; children() are auto-cleared on init().
        List<PriceCategory> cats = PriceCatalog.INSTANCE.categories();
        int y = 32;
        for (PriceCategory cat : cats) {
            final String id = cat.id();
            int count = PriceCatalog.INSTANCE.byCategory(id).size();
            Text label = Text.literal(cat.displayName() + " (" + count + ")")
                    .formatted(id.equals(currentCategoryId) ? Formatting.GOLD : Formatting.WHITE);
            addDrawableChild(ButtonWidget.builder(label, b -> {
                currentCategoryId = id;
                scroll = 0;
                clearChildren();
                init();
            }).dimensions(6, y, TAB_W, 18).build());
            y += 20;
            if (y > this.height - 60) break;
        }
    }

    private void rebuildGrid() {
        List<ItemPrice> items = PriceCatalog.INSTANCE.byCategory(currentCategoryId);
        int startX = TAB_W + 16;
        int startY = 36;
        for (int i = 0; i < ROWS * COLS; i++) {
            int idx = i + scroll * COLS;
            if (idx >= items.size()) break;
            ItemPrice p = items.get(idx);
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (TILE + GAP);
            int y = startY + row * (TILE + GAP);
            addDrawableChild(new PriceTile(x, y, TILE, p));
        }
        // Scroll buttons.
        if (scroll > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
                scroll--;
                clearChildren();
                init();
            }).dimensions(this.width - 30, startY, 24, 18).build());
        }
        int maxScroll = Math.max(0, (items.size() - 1) / COLS - ROWS + 1);
        if (scroll < maxScroll) {
            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
                scroll++;
                clearChildren();
                init();
            }).dimensions(this.width - 30, startY + TILE * ROWS - 18, 24, 18).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF101418);
        // Top bar.
        ctx.fill(0, 0, this.width, 28, 0xFF202830);
        ctx.drawText(this.textRenderer, "Catalog — " + currentCategoryId,
                8, 10, 0xFFFFFFFF, true);
        String bal = "Wallet: " + fmt(PhoneClientState.wallet) + " M$";
        ctx.drawText(this.textRenderer, bal, this.width - this.textRenderer.getWidth(bal) - 8, 10,
                0xFFA0E0A0, true);
        // Tab strip background.
        ctx.fill(0, 28, TAB_W + 6, this.height, 0xFF181C22);
        super.render(ctx, mouseX, mouseY, delta);

        // Tooltip: if mouse is over a tile, show the item + prices.
        for (var child : this.children()) {
            if (child instanceof PriceTile tile && tile.isHovered()) {
                ItemPrice p = tile.price;
                ctx.drawItemTooltip(this.textRenderer, tile.icon, mouseX, mouseY);
                Text tip = Text.empty()
                        .append(Text.literal(p.displayName()).formatted(Formatting.WHITE))
                        .append("\n")
                        .append(Text.literal("Buy:  " + p.buy()  + " M$").formatted(Formatting.GREEN))
                        .append("    ")
                        .append(Text.literal("Sell: " + p.sell() + " M$").formatted(Formatting.AQUA))
                        .append("\n")
                        .append(Text.literal("Click to buy 1").formatted(Formatting.DARK_GRAY));
                ctx.drawWrappedTooltip(this.textRenderer, splitForTooltip(tip, 250), mouseX, mouseY);
                break;
            }
        }
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    /** Splits a multi-line Text into lines for the deprecated drawTooltip-with-list overload. */
    private static java.util.List<Text> splitForTooltip(Text t, int maxWidth) {
        java.util.List<Text> out = new java.util.ArrayList<>();
        for (String s : t.getString().split("\n")) {
            out.add(Text.literal(s));
        }
        return out;
    }

    /** A single item tile in the catalog. */
    public static class PriceTile extends ButtonWidget {
        private final ItemPrice price;
        public final ItemStack icon;

        public PriceTile(int x, int y, int size, ItemPrice price) {
            super(x, y, size, size, Text.literal(price.displayName()),
                    b -> PhoneNetworking.sendBuy(price.id(), 1),
                    DEFAULT_NARRATION_SUPPLIER);
            this.price = price;
            this.icon = new ItemStack(PhoneClientState.itemOf(price.id()));
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int bg = this.isHovered() ? 0xFF3A4858 : 0xFF252D38;
            ctx.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height, bg);
            // Item icon centred.
            int iconSize = 28;
            int ix = this.getX() + (this.width - iconSize) / 2;
            int iy = this.getY() + (this.height - iconSize) / 2;
            ctx.drawItem(icon, ix, iy);
            // Tiny price tag in the corner.
            String tag = price.buy() + "$";
            int tx = this.getX() + this.width - this.textRenderer.getWidth(tag) - 3;
            int ty = this.getY() + this.height - 8;
            ctx.drawText(this.textRenderer, tag, tx, ty, 0xFFFFE070, true);
        }
    }
}
