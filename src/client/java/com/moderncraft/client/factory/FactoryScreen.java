package com.moderncraft.client.factory;

import com.moderncraft.client.phone.PhoneClientState;
import com.moderncraft.economy.factory.FactoryNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The factory working screen. Shows:
 * <ul>
 *     <li>Status line: structure valid? shift owner?</li>
 *     <li>Progress bar: ticks elapsed / shift duration</li>
 *     <li>Produced items panel: every item the factory has produced this shift,
 *         with an icon and count.</li>
 *     <li>Two buttons: "Start shift" (if no owner / you're the owner of nothing)
 *         and "End shift" (if you own the running shift).</li>
 * </ul>
 * State is pushed by the server via {@link FactoryNetworking.FactoryStatePayload}.
 */
public class FactoryScreen extends Screen {

    private static FactoryState lastState = null;
    private final BlockPos pos;

    public FactoryScreen(BlockPos pos) {
        super(Text.literal("Factory"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    public static void onState(FactoryState state) {
        lastState = state;
    }

    private void rebuildButtons() {
        boolean running = lastState != null && lastState.hasOwner();
        boolean isYou = lastState != null && lastState.isYouOwner();
        if (!running) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Start Shift").formatted(Formatting.GOLD),
                    b -> { FactoryNetworking.sendStart(pos); this.close(); })
                    .dimensions(this.width / 2 - 100, this.height - 56, 200, 20).build());
        } else if (isYou) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("End Shift & Collect").formatted(Formatting.AQUA),
                    b -> { FactoryNetworking.sendEnd(pos); this.close(); })
                    .dimensions(this.width / 2 - 120, this.height - 56, 240, 20).build());
        } else {
            // Someone else is working — just show a back button.
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(8, this.height - 26, 60, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF101418);
        ctx.fill(0, 0, this.width, 32, 0xFF202830);
        ctx.drawText(this.textRenderer, "Factory", 8, 12, 0xFFFFFFFF, true);
        String bal = "Wallet: " + fmt(PhoneClientState.wallet) + " M$";
        ctx.drawText(this.textRenderer, bal, this.width - this.textRenderer.getWidth(bal) - 8, 12,
                0xFFA0E0A0, true);

        if (lastState == null) {
            ctx.drawText(this.textRenderer, "Loading factory state...",
                    this.width / 2 - 60, this.height / 2, 0xFFC0C8D0, true);
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }
        // Status line.
        Text status;
        if (!lastState.structureValid) {
            status = Text.literal("⚠ Structure incomplete").formatted(Formatting.RED);
        } else if (!lastState.hasOwner) {
            status = Text.literal("● Ready for a shift").formatted(Formatting.GREEN);
        } else if (lastState.isYouOwner) {
            status = Text.literal("● Your shift is running").formatted(Formatting.AQUA);
        } else {
            status = Text.literal("● Another worker is on shift").formatted(Formatting.YELLOW);
        }
        ctx.drawText(this.textRenderer, status, 20, 48, 0xFFFFFFFF, true);

        // Progress bar.
        int barX = 20, barY = 70, barW = this.width - 40, barH = 16;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF202830);
        if (lastState.hasOwner) {
            int filled = (int) (barW * Math.min(1.0,
                    lastState.shiftTicks / (double) lastState.shiftDuration));
            ctx.fill(barX, barY, barX + filled, barY + barH, 0xFF4A90E2);
        }
        ctx.drawText(this.textRenderer,
                lastState.shiftTicks + " / " + lastState.shiftDuration + " ticks",
                barX + 4, barY + 4, 0xFFFFFFFF, true);

        // Produced items.
        ctx.drawText(this.textRenderer, "Produced this shift:", 20, 110,
                Formatting.WHITE, false);
        int x = 20, y = 130;
        for (int i = 0; i < lastState.producedIds.size(); i++) {
            String idStr = lastState.producedIds.get(i);
            int count = lastState.producedCounts.get(i);
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) id = Identifier.of("minecraft", idStr);
            ItemStack stack = new ItemStack(Registries.ITEM.get(id));
            ctx.drawItem(stack, x, y);
            ctx.drawText(this.textRenderer, "x" + count, x + 22, y + 8, 0xFFE0E0E0, true);
            x += 50;
            if (x > this.width - 60) {
                x = 20;
                y += 30;
            }
        }
        if (lastState.producedIds.isEmpty()) {
            ctx.drawText(this.textRenderer, "(nothing yet — wait a bit)",
                    20, 130, 0xFF808080, true);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String fmt(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    /** Lightweight mirror of FactoryStatePayload for the screen. */
    public static class FactoryState {
        public final BlockPos pos;
        public final boolean structureValid;
        public final boolean hasOwner;
        public final boolean isYouOwner;
        public final int shiftTicks;
        public final int shiftDuration;
        public final List<String> producedIds = new ArrayList<>();
        public final List<Integer> producedCounts = new ArrayList<>();
        public FactoryState(BlockPos pos, boolean sv, boolean ho, boolean iyo,
                            int st, int sd, List<String> ids, List<Integer> counts) {
            this.pos = pos; this.structureValid = sv; this.hasOwner = ho; this.isYouOwner = iyo;
            this.shiftTicks = st; this.shiftDuration = sd;
            this.producedIds.addAll(ids); this.producedCounts.addAll(counts);
        }
    }
}
