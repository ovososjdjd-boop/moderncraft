package com.moderncraft.client.bank;

import com.moderncraft.client.ModerncraftGui;
import com.moderncraft.client.phone.PhoneClientState;
import com.moderncraft.economy.bank.BankNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Stand-alone bank UI. Same flow as the phone's BankScreen but rendered as
 * a building, with a stats line showing the branch's lifetime deposits.
 */
public class BankScreen extends Screen {

    private final BlockPos pos;
    private long wallet = 0L;
    private long bank = 0L;
    private long branchTotal = 0L;
    private TextFieldWidget amountField;

    public BankScreen(BlockPos pos) {
        super(Text.literal("Bank"));
        this.pos = pos;
    }

    public void setBalances(long wallet, long bank, long branchTotal) {
        this.wallet = wallet;
        this.bank = bank;
        this.branchTotal = branchTotal;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int fieldW = 160;
        amountField = new TextFieldWidget(this.textRenderer, cx - fieldW / 2, 110, fieldW, 20,
                Text.literal("Amount"));
        amountField.setMaxLength(12);
        amountField.setText("100");
        addSelectableChild(amountField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Deposit").formatted(Formatting.GREEN),
                b -> doAction(true)).dimensions(cx - 90, 150, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Withdraw").formatted(Formatting.AQUA),
                b -> doAction(false)).dimensions(cx + 10, 150, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+100"), b -> adjust(100))
                .dimensions(cx - 100, 180, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+1k"), b -> adjust(1000))
                .dimensions(cx - 56, 180, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+10k"), b -> adjust(10000))
                .dimensions(cx - 12, 180, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Max"), b -> setMax(true))
                .dimensions(cx + 32, 180, 40, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Transfer"), b -> this.client.setScreen(new com.moderncraft.client.phone.TransferScreen()))
                .dimensions(cx - 45, 208, 90, 20).build());
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
        amountField.setText(Long.toString(deposit ? wallet : bank));
    }

    private long parseAmount() {
        try { return Long.parseLong(amountField.getText().trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private void doAction(boolean deposit) {
        long amt = parseAmount();
        if (amt <= 0) return;
        if (deposit) BankNetworking.sendDeposit(pos, amt);
        else BankNetworking.sendWithdraw(pos, amt);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(ctx, this.width, this.height);
        ModerncraftGui.header(ctx, this.textRenderer, this.width, "Bank", null);
        int cx = this.width / 2;
        String wallet = "Wallet: " + fmt(this.wallet) + " M$";
        String bank = "Bank:   " + fmt(this.bank) + " M$";
        ctx.drawText(this.textRenderer, wallet, cx - this.textRenderer.getWidth(wallet) / 2, 50,
                0xFFA0E0A0, true);
        ctx.drawText(this.textRenderer, bank, cx - this.textRenderer.getWidth(bank) / 2, 66,
                0xFF80C0E0, true);

        String stats = "Branch lifetime deposits: " + fmt(branchTotal) + " M$";
        ctx.drawText(this.textRenderer, stats,
                cx - this.textRenderer.getWidth(stats) / 2, 88, 0xFF808080, true);

        amountField.render(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }
}
