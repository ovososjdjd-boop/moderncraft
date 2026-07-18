package com.moderncraft.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/** Shared visual primitives used by all Moderncraft screens. */
public final class ModerncraftGui {
    public static final int BACKGROUND = 0xFF0E1319;
    public static final int HEADER = 0xFF1C2732;
    public static final int PANEL = 0xFF18212A;
    public static final int PANEL_ALT = 0xFF202C38;
    public static final int BORDER = 0xFF3D5264;
    public static final int TEXT = 0xFFE7EDF3;
    public static final int MUTED = 0xFF91A0AE;
    public static final int MONEY = 0xFF8BE28B;
    public static final int BUY = 0xFF72D49A;
    public static final int SELL = 0xFF78C8E8;
    public static final int WARNING = 0xFFFFD166;
    public static final int ERROR = 0xFFFF7777;

    private ModerncraftGui() {}

    public static void background(DrawContext context, int width, int height) {
        context.fill(0, 0, width, height, BACKGROUND);
    }

    public static void header(DrawContext context, TextRenderer textRenderer, int width,
                              String title, String rightText) {
        context.fill(0, 0, width, 32, HEADER);
        context.fill(0, 31, width, 32, BORDER);
        context.drawText(textRenderer, title, 10, 11, TEXT, true);
        if (rightText != null && !rightText.isBlank()) {
            context.drawText(textRenderer, rightText,
                    width - textRenderer.getWidth(rightText) - 10, 11, MONEY, true);
        }
    }

    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        context.fill(x, y, x + width, y + 1, BORDER);
        context.fill(x, y + height - 1, x + width, y + height, BORDER);
        context.fill(x, y, x + 1, y + height, BORDER);
        context.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    public static String money(long value) {
        return String.format("%,d", value).replace(',', ' ') + " M$";
    }

    public static String balance(long wallet, long bank) {
        return "Wallet: " + money(wallet) + "  |  Bank: " + money(bank);
    }
}
