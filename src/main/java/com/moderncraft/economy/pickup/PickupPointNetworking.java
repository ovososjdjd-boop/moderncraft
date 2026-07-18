package com.moderncraft.economy.pickup;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.price.PriceCatalogView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Packets for the Pickup Point flow.
 * <ul>
 *     <li>{@code OpenScreenPayload} (S2C) — server tells the client "open the
 *         Pickup Point UI". The client already has the player's inventory, so
 *         we just need a trigger + the position for context.</li>
 *     <li>{@code SellItemPayload} (C2S) — client says "sell N of item id".
 *         Server validates price, removes items, credits wallet.</li>
 *     <li>{@code SellAllPayload} (C2S) — sell every priced item in the
 *         player's inventory. Convenient for big haul after a mining trip.</li>
 * </ul>
 */
public final class PickupPointNetworking {

    private static boolean commonRegistered = false;

    public static final Identifier OPEN_ID = Moderncraft.id("pickup_open");
    public static final Identifier SELL_ID = Moderncraft.id("pickup_sell");
    public static final Identifier SELL_ALL_ID = Moderncraft.id("pickup_sell_all");
    public static final Identifier COLLECT_ID = Moderncraft.id("pickup_collect");
    public static final Identifier ORDERS_ID = Moderncraft.id("pickup_orders");

    public record OpenScreenPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<OpenScreenPayload> ID = new CustomPayload.Id<>(OPEN_ID);
        public static final PacketCodec<PacketByteBuf, OpenScreenPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> buf.writeBlockPos(p.pos),
                        buf -> new OpenScreenPayload(buf.readBlockPos())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SellItemPayload(String itemId, int count) implements CustomPayload {
        public static final CustomPayload.Id<SellItemPayload> ID = new CustomPayload.Id<>(SELL_ID);
        public static final PacketCodec<PacketByteBuf, SellItemPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeString(p.itemId);
                            buf.writeInt(p.count);
                        },
                        buf -> new SellItemPayload(buf.readString(), buf.readInt())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SellAllPayload() implements CustomPayload {
        public static final CustomPayload.Id<SellAllPayload> ID = new CustomPayload.Id<>(SELL_ALL_ID);
        public static final PacketCodec<PacketByteBuf, SellAllPayload> CODEC =
                PacketCodec.unit(new SellAllPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CollectOrdersPayload() implements CustomPayload {
        public static final CustomPayload.Id<CollectOrdersPayload> ID = new CustomPayload.Id<>(COLLECT_ID);
        public static final PacketCodec<PacketByteBuf, CollectOrdersPayload> CODEC =
                PacketCodec.unit(new CollectOrdersPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Compact client sync: one line per order, item id and count separated by '|'. */
    public record PendingOrdersPayload(String encoded) implements CustomPayload {
        public static final CustomPayload.Id<PendingOrdersPayload> ID = new CustomPayload.Id<>(ORDERS_ID);
        public static final PacketCodec<PacketByteBuf, PendingOrdersPayload> CODEC =
                PacketCodec.of((p, buf) -> buf.writeString(p.encoded), buf -> new PendingOrdersPayload(buf.readString(32767)));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerCommon() {
        if (commonRegistered) return;
        commonRegistered = true;
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SellItemPayload.ID, SellItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SellAllPayload.ID, SellAllPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CollectOrdersPayload.ID, CollectOrdersPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PendingOrdersPayload.ID, PendingOrdersPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SellItemPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (findPickupNear(player) == null) {
                PhoneNetworking.sendError(player, "You must be at a Pickup Point to sell items.");
                return;
            }

            String idStr = payload.itemId();
            int wanted = Math.max(1, payload.count());

            Identifier ident = Identifier.tryParse(idStr);
            if (ident == null) ident = Identifier.of("minecraft", idStr);
            Item item = Registries.ITEM.get(ident);
            if (item == null) {
                PhoneNetworking.sendError(player, "Unknown item: " + idStr);
                return;
            }

            Optional<com.moderncraft.economy.price.ItemPrice> opt = PriceCatalogView.lookupById(idStr);
            if (opt.isEmpty()) {
                PhoneNetworking.sendError(player, "No buy/sell price for " + idStr + ".");
                return;
            }
            var price = opt.get();
            if (price.sell() <= 0) {
                PhoneNetworking.sendError(player, price.displayName() + " cannot be sold.");
                return;
            }

            // Count how many the player actually has.
            int have = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == item) have += stack.getCount();
            }
            if (have == 0) {
                PhoneNetworking.sendError(player, "You don't have any " + price.displayName() + ".");
                return;
            }
            int toSell = Math.min(wanted, have);

            // Remove from inventory.
            int remaining = toSell;
            for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == item) {
                    int take = Math.min(stack.getCount(), remaining);
                    stack.decrement(take);
                    remaining -= take;
                }
            }
            int actuallySold = toSell - remaining;
            if (actuallySold == 0) {
                PhoneNetworking.sendError(player, "Nothing sold.");
                return;
            }

            long earned = (long) price.sell() * actuallySold;
            com.moderncraft.economy.state.EconomyService.creditWallet(server, player.getUuid(), earned);

            // Bump the block-entity's totalSold stat if we can find it.
            BlockPos pos = findPickupNear(player);
            if (pos != null) {
                if (player.getWorld().getBlockEntity(pos) instanceof PickupPointBlockEntity pe) {
                    pe.addToTotal(earned);
                }
            }

            PhoneNetworking.sendInfo(player, "Sold " + actuallySold + " × " + price.displayName()
                    + " for " + earned + " M$.");
            PhoneNetworking.syncBalances(player, server);
        });

        ServerPlayNetworking.registerGlobalReceiver(SellAllPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (findPickupNear(player) == null) {
                PhoneNetworking.sendError(player, "You must be at a Pickup Point to sell items.");
                return;
            }

            long totalEarned = 0L;
            int totalItems = 0;
            // Walk the inventory; sell anything that has a positive price and
            // isn't an admin item (we treat sell==0 as "do not sell").
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                Identifier id = Registries.ITEM.getId(stack.getItem());
                if (id == null) continue;
                Optional<com.moderncraft.economy.price.ItemPrice> opt = PriceCatalogView.lookup(id);
                if (opt.isEmpty()) continue;
                var price = opt.get();
                if (price.sell() <= 0) continue;

                int n = stack.getCount();
                long earned = (long) price.sell() * n;
                totalEarned += earned;
                totalItems += n;
                stack.setCount(0);
            }
            if (totalItems == 0) {
                PhoneNetworking.sendInfo(player, "Nothing in your inventory has a sell price.");
                return;
            }
            com.moderncraft.economy.state.EconomyService.creditWallet(server, player.getUuid(), totalEarned);

            BlockPos pos = findPickupNear(player);
            if (pos != null) {
                if (player.getWorld().getBlockEntity(pos) instanceof PickupPointBlockEntity pe) {
                    pe.addToTotal(totalEarned);
                }
            }

            PhoneNetworking.sendInfo(player, "Bulk-sold " + totalItems + " items for " + totalEarned + " M$.");
            PhoneNetworking.syncBalances(player, server);
        });

        ServerPlayNetworking.registerGlobalReceiver(CollectOrdersPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (findPickupNear(player) == null) {
                PhoneNetworking.sendError(player, "You must be at a Pickup Point to collect orders.");
                return;
            }
            var state = com.moderncraft.economy.state.WorldEconomyState.get(server);
            var orders = state.takeOrders(player.getUuid());
            if (orders.isEmpty()) {
                PhoneNetworking.sendInfo(player, "You have no paid orders waiting at the Pickup Point.");
                syncOrders(player, server);
                return;
            }
            int delivered = 0;
            for (var order : orders) {
                Identifier itemId = Identifier.tryParse(order.itemId());
                Item item = itemId == null ? null : Registries.ITEM.get(itemId);
                if (item == null) continue;
                ItemStack stack = new ItemStack(item, order.count());
                player.getInventory().insertStack(stack);
                int received = order.count() - stack.getCount();
                delivered += received;
                if (stack.getCount() > 0) {
                    state.addOrder(player.getUuid(), new com.moderncraft.economy.state.PurchaseOrder(order.itemId(), stack.getCount()));
                }
            }
            PhoneNetworking.sendInfo(player, delivered > 0
                    ? "Collected " + delivered + " items from paid orders."
                    : "Your inventory is full; orders remain at the Pickup Point.");
            syncOrders(player, server);
        });
    }

    // --- helpers ------------------------------------------------------------

    /**
     * Looks for a Pickup Point block within 6 blocks of the player. We use
     * this to update the right block-entity's totalSold stat. This is a
     * best-effort heuristic — if multiple points are nearby, we just pick the
     * closest one. Good enough for the stat counter; doesn't affect gameplay.
     */
    private static BlockPos findPickupNear(ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (player.getWorld().getBlockState(cursor).getBlock()
                            instanceof PickupPointBlock) {
                        double dist = cursor.getSquaredDistance(origin);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = cursor.toImmutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    // --- client side --------------------------------------------------------

    public static void sendSell(String itemId, int count) {
        ClientPlayNetworking.send(new SellItemPayload(itemId, count));
    }

    public static void sendSellAll() {
        ClientPlayNetworking.send(new SellAllPayload());
    }

    public static void sendOpen(ServerPlayerEntity player, BlockPos pos) {
        ServerPlayNetworking.send(player, new OpenScreenPayload(pos));
        syncOrders(player, player.getServer());
    }

    public static void sendCollect() {
        ClientPlayNetworking.send(new CollectOrdersPayload());
    }

    public static void syncOrders(ServerPlayerEntity player, MinecraftServer server) {
        var orders = com.moderncraft.economy.state.WorldEconomyState.get(server).pendingOrders(player.getUuid());
        StringBuilder encoded = new StringBuilder();
        for (var order : orders) {
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(order.itemId()).append('|').append(order.count());
        }
        ServerPlayNetworking.send(player, new PendingOrdersPayload(encoded.toString()));
    }
}
