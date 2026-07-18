package com.moderncraft.client.phone;

import com.moderncraft.client.ModerncraftGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Paid phone orders. Collection is intentionally only possible at a physical Pickup Point. */
public final class OrdersScreen extends Screen {
    public OrdersScreen() { super(Text.literal("Orders")); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(8, this.height - 28, 70, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ModerncraftGui.background(context, this.width, this.height);
        ModerncraftGui.header(context, this.textRenderer, this.width, "Orders",
                ModerncraftGui.balance(PhoneClientState.wallet, PhoneClientState.bank));

        int total = PhoneClientState.pendingOrders.stream().mapToInt(PhoneClientState.PendingOrder::count).sum();
        context.drawText(this.textRenderer, total == 0
                        ? "No paid orders are waiting."
                        : "Paid orders waiting at a Pickup Point: " + total,
                14, 46, total == 0 ? ModerncraftGui.MUTED : ModerncraftGui.WARNING, true);

        if (total == 0) {
            context.drawText(this.textRenderer, "Buy an item in Catalog, then collect it at a village Pickup Point.",
                    14, 70, ModerncraftGui.MUTED, false);
        } else {
            int y = 68;
            for (PhoneClientState.PendingOrder order : PhoneClientState.pendingOrders) {
                ItemStack icon = new ItemStack(PhoneClientState.itemOf(order.itemId()));
                ModerncraftGui.panel(context, 12, y, this.width - 24, 34);
                context.drawItem(icon, 20, y + 4);
                String name = icon.getName().getString();
                context.drawText(this.textRenderer, name + " × " + order.count(), 52, y + 6,
                        ModerncraftGui.TEXT, true);
                context.drawText(this.textRenderer, "Paid · Waiting at Pickup Point", 52, y + 19,
                        ModerncraftGui.SELL, false);
                y += 40;
                if (y > this.height - 48) break;
            }
            context.drawText(this.textRenderer, "Go to a Pickup Point to collect these orders.",
                    14, this.height - 46, ModerncraftGui.WARNING, true);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
