package com.moderncraft.client.phone;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The "home screen" of the phone: shows 4 app icons and a balance bar at the top.
 * Each icon is a real {@link ButtonWidget} (no textures — we use item icons
 * as the "app" art and a coloured rectangle as the tile background).
 */
public class PhoneHomeScreen extends Screen {

    public static final Identifier BG = Identifier.of("moderncraft", "textures/gui/phone/home.png");

    private static final int TILE_SIZE = 64;
    private static final int TILE_PAD = 16;
    private static final int COLS = 2;
    private static final int ROWS = 2;

    public PhoneHomeScreen() {
        super(Text.literal("Phone"));
    }

    @Override
    protected void init() {
        // Four apps in a 2x2 grid in the middle of the screen.
        int gridW = COLS * TILE_SIZE + (COLS - 1) * TILE_PAD;
        int gridH = ROWS * TILE_SIZE + (ROWS - 1) * TILE_PAD;
        int startX = (this.width - gridW) / 2;
        int startY = (this.height - gridH) / 2;

        addApp(startX, startY, Items.CATALOG, Text.literal("Catalog"), b -> openCatalog());
        addApp(startX + TILE_SIZE + TILE_PAD, startY, Items.BANK, Text.literal("Bank"), b -> openBank());
        addApp(startX, startY + TILE_SIZE + TILE_PAD, Items.STOCK, Text.literal("Stock"), b -> openStock());
        addApp(startX + TILE_SIZE + TILE_PAD, startY + TILE_SIZE + TILE_PAD,
                Items.JOBS, Text.literal("Jobs"), b -> openJobs());

        // Close button.
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(this.width / 2 - 40, this.height - 30, 80, 20)
                .build());
    }

    private void addApp(int x, int y, ItemStack icon, Text label, ButtonWidget.PressAction onPress) {
        addDrawableChild(new AppTileButton(x, y, TILE_SIZE, TILE_SIZE, icon, label, onPress));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Background — solid dark colour so the phone feels "on".
        ctx.fill(0, 0, this.width, this.height, 0xFF101418);

        // Status bar at the top: title + balance.
        ctx.fill(0, 0, this.width, 36, 0xFF202830);
        ctx.drawText(this.textRenderer, "Phone", 10, 12, 0xFFFFFFFF, true);
        String bal = "Wallet: " + fmt(PhoneClientState.wallet) + " M$  |  Bank: " + fmt(PhoneClientState.bank) + " M$";
        ctx.drawText(this.textRenderer, bal, this.width - this.textRenderer.getWidth(bal) - 10, 12,
                0xFFA0E0A0, true);

        // Each app tile is drawn by the AppTileButton itself, but we render the
        // label below the grid here for clarity.
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    // --- app launchers ------------------------------------------------------

    private void openCatalog() {
        this.client.setScreen(new CatalogScreen());
    }

    private void openBank() {
        this.client.setScreen(new BankScreen());
    }

    private void openStock() {
        com.moderncraft.economy.stock.StockNetworking.sendOpenRequest();
    }

    private void openJobs() {
        this.client.setScreen(new PlaceholderScreen("Jobs",
                Text.literal("Courier: take an order at the cafe and deliver it to a villager. " +
                        "Loader: accept a heavy-load order and deliver it to the highlighted target. " +
                        "Factory: start a shift at the factory base.")));
    }

    // --- inner types --------------------------------------------------------

    /** Hard-coded "app icons" — vanilla items stand in for our future custom ones. */
    public static final class Items {
        public static final ItemStack CATALOG = new ItemStack(net.minecraft.item.Items.CHEST);
        public static final ItemStack BANK    = new ItemStack(net.minecraft.item.Items.GOLD_INGOT);
        public static final ItemStack STOCK   = new ItemStack(net.minecraft.item.Items.EMERALD);
        public static final ItemStack JOBS    = new ItemStack(net.minecraft.item.Items.IRON_PICKAXE);
        private Items() {}
    }

    /** A button that draws itself as a tile with an item icon and a label. */
    public static class AppTileButton extends ButtonWidget {
        private final ItemStack icon;
        public AppTileButton(int x, int y, int w, int h, ItemStack icon, Text label,
                             PressAction onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.icon = icon;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            // Tile background — slightly lighter when hovered.
            int bg = this.isHovered() ? 0xFF3A4858 : 0xFF252D38;
            ctx.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height, bg);

            // 1px border.
            ctx.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + 1, 0xFF505A68);
            ctx.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, 0xFF505A68);
            ctx.fill(this.getX(), this.getY(),
                    this.getX() + 1, this.getY() + this.height, 0xFF505A68);
            ctx.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, 0xFF505A68);

            // Item icon, centred.
            int iconSize = 32;
            int ix = this.getX() + (this.width - iconSize) / 2;
            int iy = this.getY() + (this.height - iconSize) / 2 - 6;
            ctx.drawItem(icon, ix, iy);

            // Label below the icon.
            int labelY = this.getY() + this.height - 12;
            int labelW = this.textRenderer.getWidth(this.getMessage());
            int labelX = this.getX() + (this.width - labelW) / 2;
            ctx.drawText(this.textRenderer, this.getMessage(), labelX, labelY, 0xFFE0E8F0, true);
        }
    }
}
