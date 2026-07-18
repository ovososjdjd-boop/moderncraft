package com.moderncraft.client.phone;

import com.moderncraft.client.ModerncraftGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Economy journal screen. The authoritative rows are synchronized by the server in a later snapshot packet. */
public final class HistoryScreen extends Screen {
    public HistoryScreen() { super(Text.translatable("gui.moderncraft.history")); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.moderncraft.back"), button -> close())
                .dimensions(8, this.height - 28, 70, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(context, this.width, this.height);
        ModerncraftGui.header(context, this.textRenderer, this.width, "History",
                ModerncraftGui.balance(PhoneClientState.wallet, PhoneClientState.bank));
        context.drawText(this.textRenderer, "Recent wallet and bank operations", 14, 46,
                ModerncraftGui.MUTED, false);
        int y = 66;
        for (int i = PhoneClientState.history.size() - 1; i >= 0 && y < this.height - 42; i--) {
            PhoneClientState.HistoryEntry entry = PhoneClientState.history.get(i);
            boolean positive = entry.type().equals("credit") || entry.type().equals("deposit")
                    || entry.type().equals("transfer_in");
            ModerncraftGui.panel(context, 12, y, this.width - 24, 28);
            String sign = positive ? "+" : "−";
            int color = positive ? ModerncraftGui.BUY : ModerncraftGui.SELL;
            context.drawText(this.textRenderer, sign + ModerncraftGui.money(entry.amount()), 20, y + 5, color, true);
            context.drawText(this.textRenderer, entry.type() + " · " + entry.note(), 145, y + 5,
                    ModerncraftGui.TEXT, false);
            context.drawText(this.textRenderer, "tick " + entry.timestamp(), 145, y + 16,
                    ModerncraftGui.MUTED, false);
            y += 32;
        }
        if (PhoneClientState.history.isEmpty()) {
            context.drawText(this.textRenderer, "No transactions yet.", 14, 70, ModerncraftGui.MUTED, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
