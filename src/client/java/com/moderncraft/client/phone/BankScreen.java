package com.moderncraft.client.phone;

import com.moderncraft.economy.phone.PhoneNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Bank screen — shows wallet + bank balances and lets the user deposit or
 * withdraw a custom amount. We do the actual transaction on the server.
 */
public class BankScreen extends Screen {

    private TextFieldWidget amountField;

    public BankScreen() {
        super(Text.literal("Bank"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int fieldW = 160;
        amountField = new TextFieldWidget(this.textRenderer, cx - fieldW / 2, 90, fieldW, 20,
                Text.literal("Amount"));
        amountField.setMaxLength(12);
        amountField.setText("100");
        addSelectableChild(amountField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Deposit").formatted(Formatting.GREEN), b -> doAction(true))
                .dimensions(cx - 90, 130, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Withdraw").formatted(Formatting.AQUA), b -> doAction(false))
                .dimensions(cx + 10, 130, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+100"), b -> adjust(100))
                .dimensions(cx - 100, 160, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+1k"), b -> adjust(1000))
                .dimensions(cx - 56, 160, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+10k"), b -> adjust(10000))
                .dimensions(cx - 12, 160, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Max"), b -> setMax(true))
                .dimensions(cx + 32, 160, 40, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(8, this.height - 26, 60, 20).build());
    }

    private void adjust(long delta) {
        long cur = parseAmount();
        long next = cur + delta;
        if (next < 0) next = 0;
        amountField.setText(Long.toString(next));
    }

    private void setMax(boolean deposit) {
        long max = deposit ? PhoneClientState.wallet : PhoneClientState.bank;
        amountField.setText(Long.toString(max));
    }

    private long parseAmount() {
        try {
            return Long.parseLong(amountField.getText().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void doAction(boolean deposit) {
        long amount = parseAmount();
        if (amount <= 0) return;
        PhoneNetworking.sendBank(deposit, amount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF101418);
        ctx.fill(0, 0, this.width, 28, 0xFF202830);
        ctx.drawText(this.textRenderer, "Bank", 8, 10, 0xFFFFFFFF, true);

        // Balances.
        String wallet = "Wallet: " + fmt(PhoneClientState.wallet) + " M$";
        String bank = "Bank:   " + fmt(PhoneClientState.bank) + " M$";
        int cx = this.width / 2;
        ctx.drawText(this.textRenderer, wallet, cx - this.textRenderer.getWidth(wallet) / 2, 40,
                0xFFA0E0A0, true);
        ctx.drawText(this.textRenderer, bank, cx - this.textRenderer.getWidth(bank) / 2, 56,
                0xFF80C0E0, true);

        amountField.render(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }
}
