package com.moderncraft.client.phone;

import com.moderncraft.client.ModerncraftGui;
import com.moderncraft.economy.phone.PhoneNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Wallet-to-wallet transfer screen. The server remains authoritative. */
public final class TransferScreen extends Screen {
    private TextFieldWidget playerField;
    private TextFieldWidget amountField;

    public TransferScreen() { super(Text.translatable("gui.moderncraft.transfer")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        playerField = new TextFieldWidget(this.textRenderer, cx - 100, 82, 200, 20, Text.literal("Player name"));
        playerField.setMaxLength(16);
        addSelectableChild(playerField);
        amountField = new TextFieldWidget(this.textRenderer, cx - 100, 122, 200, 20, Text.literal("Amount"));
        amountField.setMaxLength(12);
        amountField.setText("100");
        addSelectableChild(amountField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Send").formatted(Formatting.GREEN), b -> send())
                .dimensions(cx - 100, 158, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.moderncraft.back"), b -> close())
                .dimensions(8, this.height - 28, 70, 20).build());
    }

    private void send() {
        String player = playerField.getText().trim();
        long amount;
        try { amount = Long.parseLong(amountField.getText().trim()); }
        catch (NumberFormatException ignored) { return; }
        if (!player.isBlank() && amount > 0) PhoneNetworking.sendTransfer(player, amount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(context, this.width, this.height);
        ModerncraftGui.header(context, this.textRenderer, this.width, "Transfer",
                ModerncraftGui.money(PhoneClientState.wallet));
        context.drawText(this.textRenderer, "Send money to an online player", this.width / 2 - 100, 52,
                ModerncraftGui.MUTED, false);
        context.drawText(this.textRenderer, "Player name", this.width / 2 - 100, 70, ModerncraftGui.TEXT, true);
        context.drawText(this.textRenderer, "Amount in M$", this.width / 2 - 100, 110, ModerncraftGui.TEXT, true);
        playerField.render(context, mouseX, mouseY, delta);
        amountField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
