package com.moderncraft.client.phone;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** In-game job directory. Jobs are accepted at their physical buildings/NPCs. */
public final class JobsScreen extends Screen {
    public JobsScreen() { super(Text.literal("Moderncraft Jobs")); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(8, this.height - 28, 70, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF101418);
        context.fill(0, 0, this.width, 34, 0xFF202830);
        context.drawText(this.textRenderer, "Jobs", 10, 12, 0xFFFFFFFF, true);

        int x = this.width / 2 - 150;
        int y = 52;
        drawJob(context, x, y, "Courier", "Cafe Courier", "Take a parcel at the cafe and deliver it to the assigned resident.", 0xFFFFD166);
        drawJob(context, x, y + 62, "Loader", "Loader Depot", "Carry heavy blocks or furniture to the highlighted delivery target.", 0xFFD28CFF);
        drawJob(context, x, y + 124, "Factory worker", "Factory", "Start a three-minute shift and manufacture redstone parts.", 0xFF75D6A2);
        drawJob(context, x, y + 186, "Trader", "Stock Exchange", "Buy shares, watch prices and sell when the market rises.", 0xFF7CC7FF);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawJob(DrawContext context, int x, int y, String title, String place,
                         String description, int color) {
        context.fill(x, y, x + 300, y + 52, 0xFF1B222B);
        context.fill(x, y, x + 4, y + 52, color);
        context.drawText(this.textRenderer, title, x + 14, y + 8, color, true);
        context.drawText(this.textRenderer, place, x + 14, y + 21, 0xFFE0E8F0, true);
        String[] words = description.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y + 35;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.textRenderer.getWidth(candidate) > 270) {
                context.drawText(this.textRenderer, line.toString(), x + 14, lineY, 0xFF9AA7B5, false);
                line.setLength(0);
                line.append(word);
                lineY += 10;
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) context.drawText(this.textRenderer, line.toString(), x + 14, lineY, 0xFF9AA7B5, false);
    }
}
