package com.moderncraft.client.stock;

import com.moderncraft.client.ModerncraftGui;
import com.moderncraft.economy.stock.StockNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Stock exchange UI: list of 5 companies, current price, share count, and
 * a small "buy 1" / "sell 1" pair of buttons per row. Wallet is shown
 * at the top.
 */
public class StockScreen extends Screen {

    private final List<Company> companies = new ArrayList<>();
    private long wallet = 0L;
    private TextFieldWidget quantityField;

    public StockScreen() {
        super(Text.literal("Stock Exchange"));
    }

    public void setSnapshot(List<String> ids, List<String> names, List<Long> prices,
                            List<Integer> shareCounts, long wallet) {
        this.companies.clear();
        for (int i = 0; i < ids.size(); i++) {
            this.companies.add(new Company(ids.get(i), names.get(i), prices.get(i), shareCounts.get(i)));
        }
        this.wallet = wallet;
    }

    @Override
    protected void init() {
        quantityField = new TextFieldWidget(this.textRenderer, this.width / 2 - 45, 36, 90, 20,
                Text.literal("Shares"));
        quantityField.setMaxLength(7);
        quantityField.setText("1");
        addSelectableChild(quantityField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Max affordable"), b -> {
            quantityField.setText(Long.toString(Math.max(1L, wallet / Math.max(1L, companies.isEmpty() ? 1L : companies.get(0).price))));
        }).dimensions(this.width / 2 + 50, 36, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(8, this.height - 26, 60, 20).build());
        // Per-company rows.
        int top = 70;
        int rowH = 36;
        for (int i = 0; i < companies.size(); i++) {
            Company c = companies.get(i);
            int y = top + i * rowH;
            int idx = i;
            // Buy 1
            addDrawableChild(ButtonWidget.builder(Text.literal("Buy 1").formatted(Formatting.GREEN),
                    b -> StockNetworking.sendBuy(c.id, quantity()))
                    .dimensions(this.width - 130, y, 50, 18).build());
            // Sell 1
            addDrawableChild(ButtonWidget.builder(Text.literal("Sell 1").formatted(Formatting.AQUA),
                    b -> StockNetworking.sendSell(c.id, quantity()))
                    .dimensions(this.width - 75, y, 50, 18).build());
        }
    }

    private int quantity() {
        try { return Math.max(1, Math.min(1_000_000, Integer.parseInt(quantityField.getText().trim()))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(ctx, this.width, this.height);
        ModerncraftGui.header(ctx, this.textRenderer, this.width, "Stock Exchange",
                ModerncraftGui.money(wallet));
        ctx.drawText(this.textRenderer, "Shares to trade:", this.width / 2 - 150, 42, 0xFF91A0AE, true);
        int top = 70;
        int rowH = 36;
        for (int i = 0; i < companies.size(); i++) {
            Company c = companies.get(i);
            int y = top + i * rowH;
            int bg = (i % 2 == 0) ? 0xFF181C22 : 0xFF1A1F26;
            ctx.fill(8, y, this.width - 140, y + rowH - 4, bg);
            ctx.drawText(this.textRenderer, c.name, 16, y + 4, 0xFFFFFFFF, true);
            ctx.drawText(this.textRenderer, c.id, 16, y + 16, 0xFF606060, true);
            String price = fmt(c.price) + " M$";
            ctx.drawText(this.textRenderer, price, this.width - 250, y + 6, 0xFFFFE070, true);
            String shares = "you own: " + c.shares;
            ctx.drawText(this.textRenderer, shares, this.width - 250, y + 18, 0xFF80C0E0, true);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    private record Company(String id, String name, long price, int shares) {}
}
