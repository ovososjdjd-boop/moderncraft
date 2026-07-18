package com.moderncraft.client.phone;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Shown when a player taps Stock or Jobs. The screen has a "coming soon"
 * message and a back button. We don't need to communicate with the server.
 */
public class PlaceholderScreen extends Screen {

    public PlaceholderScreen(String title, Text message) {
        super(Text.literal(title));
        this.message = message;
    }

    private final Text message;

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(this.width / 2 - 30, this.height - 36, 60, 20)
                .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF101418);
        ctx.fill(0, 0, this.width, 28, 0xFF202830);
        ctx.drawText(this.textRenderer, this.title.getString(), 8, 10, 0xFFFFFFFF, true);
        // Wrap message manually.
        int w = Math.min(this.width - 40, 360);
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = message.getString().split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = (cur.length() == 0 ? "" : " ") + word;
            if (this.textRenderer.getWidth(cur + test) > w) {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            } else {
                cur.append(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        int y = this.height / 2 - lines.size() * 5;
        for (String line : lines) {
            int x = (this.width - this.textRenderer.getWidth(line)) / 2;
            ctx.drawText(this.textRenderer, line, x, y, 0xFFC0C8D0, true);
            y += 12;
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
