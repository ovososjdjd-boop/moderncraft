package com.moderncraft.economy.phone;

import com.moderncraft.Moderncraft;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All packet types for the phone flow. We define them as records (which
 * implement {@link CustomPayload} automatically) and register them on both
 * sides through {@link PayloadTypeRegistry}.
 * <p>
 * Payload types:
 * <ul>
 *     <li>{@code OpenPhoneRequestPayload} (C2S) — client says "I want to open
 *         the phone". Server validates and replies with {@code OpenPhoneAck}.</li>
 *     <li>{@code OpenPhoneAckPayload} (S2C) — server grants opening, carries
 *         the player's wallet and bank balances.</li>
 *     <li>{@code BuyItemPayload} (C2S) — client asks to buy N of an item by
 *         its namespaced id. Server replies with {@code BalanceSyncPayload}.</li>
 *     <li>{@code BalanceSyncPayload} (S2C) — server pushes current balances
 *         so the client UI can update after a transaction.</li>
 *     <li>{@code BankActionPayload} (C2S) — deposit/withdraw N from bank.</li>
 *     <li>{@code PhoneMessagePayload} (S2C) — error or info toast text.</li>
 * </ul>
 */
public final class PhoneNetworking {

    private static boolean commonRegistered = false;

    private PhoneNetworking() {}

    public static final Identifier OPEN_REQUEST_ID = Moderncraft.id("phone_open_request");
    public static final Identifier OPEN_ACK_ID    = Moderncraft.id("phone_open_ack");
    public static final Identifier BUY_ID         = Moderncraft.id("phone_buy");
    public static final Identifier BALANCE_ID     = Moderncraft.id("phone_balance");
    public static final Identifier BANK_ID        = Moderncraft.id("phone_bank");
    public static final Identifier MESSAGE_ID     = Moderncraft.id("phone_message");

    // --- record definitions -------------------------------------------------

    public record OpenPhoneRequestPayload() implements CustomPayload {
        public static final CustomPayload.Id<OpenPhoneRequestPayload> ID =
                new CustomPayload.Id<>(OPEN_REQUEST_ID);
        public static final PacketCodec<PacketByteBuf, OpenPhoneRequestPayload> CODEC =
                PacketCodec.unit(new OpenPhoneRequestPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenPhoneAckPayload(long wallet, long bank) implements CustomPayload {
        public static final CustomPayload.Id<OpenPhoneAckPayload> ID =
                new CustomPayload.Id<>(OPEN_ACK_ID);
        public static final PacketCodec<PacketByteBuf, OpenPhoneAckPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeLong(p.wallet);
                            buf.writeLong(p.bank);
                        },
                        buf -> new OpenPhoneAckPayload(buf.readLong(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BuyItemPayload(String itemId, int count) implements CustomPayload {
        public static final CustomPayload.Id<BuyItemPayload> ID =
                new CustomPayload.Id<>(BUY_ID);
        public static final PacketCodec<PacketByteBuf, BuyItemPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeString(p.itemId);
                            buf.writeInt(p.count);
                        },
                        buf -> new BuyItemPayload(buf.readString(), buf.readInt())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BalanceSyncPayload(long wallet, long bank) implements CustomPayload {
        public static final CustomPayload.Id<BalanceSyncPayload> ID =
                new CustomPayload.Id<>(BALANCE_ID);
        public static final PacketCodec<PacketByteBuf, BalanceSyncPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeLong(p.wallet);
                            buf.writeLong(p.bank);
                        },
                        buf -> new BalanceSyncPayload(buf.readLong(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BankActionPayload(boolean deposit, long amount) implements CustomPayload {
        public static final CustomPayload.Id<BankActionPayload> ID =
                new CustomPayload.Id<>(BANK_ID);
        public static final PacketCodec<PacketByteBuf, BankActionPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeBoolean(p.deposit);
                            buf.writeLong(p.amount);
                        },
                        buf -> new BankActionPayload(buf.readBoolean(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PhoneMessagePayload(String text, boolean isError) implements CustomPayload {
        public static final CustomPayload.Id<PhoneMessagePayload> ID =
                new CustomPayload.Id<>(MESSAGE_ID);
        public static final PacketCodec<PacketByteBuf, PhoneMessagePayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeString(p.text);
                            buf.writeBoolean(p.isError);
                        },
                        buf -> new PhoneMessagePayload(buf.readString(), buf.readBoolean())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // --- registration -------------------------------------------------------

    public static void registerCommon() {
        if (commonRegistered) return;
        commonRegistered = true;
        PayloadTypeRegistry.playC2S().register(OpenPhoneRequestPayload.ID, OpenPhoneRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyItemPayload.ID, BuyItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BankActionPayload.ID, BankActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPhoneAckPayload.ID, OpenPhoneAckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.ID, BalanceSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PhoneMessagePayload.ID, PhoneMessagePayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(OpenPhoneRequestPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            // Acknowledge with current balances.
            var account = com.moderncraft.economy.state.EconomyService.view(
                    player.getServer(), player.getUuid());
            ServerPlayNetworking.send(player,
                    new OpenPhoneAckPayload(account.wallet(), account.bank()));
            // The client opens the screen on receiving the ACK.
        });

        ServerPlayNetworking.registerGlobalReceiver(BuyItemPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            String id = payload.itemId();
            int count = Math.min(Math.max(1, payload.count()), 2304);
            var priceOpt = com.moderncraft.economy.price.PriceCatalogView.lookupById(id);
            if (priceOpt.isEmpty()) {
                sendError(player, "Item '" + id + "' is not sold in the catalog.");
                return;
            }
            var price = priceOpt.get();
            if (price.buy() <= 0) {
                sendError(player, price.displayName() + " cannot be purchased.");
                return;
            }
            Identifier itemIdentifier = Identifier.tryParse(id);
            Item item = itemIdentifier == null ? null : Registries.ITEM.get(itemIdentifier);
            if (item == null) {
                sendError(player, "Item id not in registry: " + id);
                return;
            }
            var state = com.moderncraft.economy.state.WorldEconomyState.get(server);
            if (!state.canAcceptOrder(player.getUuid(), count)) {
                sendError(player, "Your pickup order queue is full. Collect existing orders first.");
                return;
            }
            long total = (long) price.buy() * count;
            var result = com.moderncraft.economy.state.EconomyService.debitWallet(
                    server, player.getUuid(), total);
            if (result != com.moderncraft.economy.state.EconomyService.Result.OK) {
                sendError(player, switch (result) {
                    case NOT_ENOUGH_WALLET -> "Not enough money. Need " + total + " M$.";
                    case INVALID_AMOUNT -> "Invalid amount.";
                    default -> "Transaction failed.";
                });
                syncBalances(player, server);
                return;
            }
            // Purchases are paid for immediately but remain at the pickup point.
            // This prevents remote buying from becoming an inventory teleport and
            // gives the delivery system a real economic role.
            state.addOrder(player.getUuid(), new com.moderncraft.economy.state.PurchaseOrder(id, count));
            sendInfo(player, "Order placed: " + count + " × " + price.displayName()
                    + ". Collect it at a Pickup Point.");
            syncBalances(player, server);
        });

        ServerPlayNetworking.registerGlobalReceiver(BankActionPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            var result = payload.deposit()
                    ? com.moderncraft.economy.state.EconomyService.depositToBank(
                            server, player.getUuid(), payload.amount())
                    : com.moderncraft.economy.state.EconomyService.withdrawFromBank(
                            server, player.getUuid(), payload.amount());
            if (result == com.moderncraft.economy.state.EconomyService.Result.OK) {
                sendInfo(player, payload.deposit()
                        ? "Deposited " + payload.amount() + " M$ into the bank."
                        : "Withdrew " + payload.amount() + " M$ from the bank.");
            } else {
                sendError(player, switch (result) {
                    case NOT_ENOUGH_WALLET -> "Not enough money in wallet.";
                    case NOT_ENOUGH_BANK -> "Not enough money in bank.";
                    case INVALID_AMOUNT -> "Amount must be positive.";
                    default -> "Bank action failed.";
                });
            }
            syncBalances(player, server);
        });
    }

    public static void sendOpenRequest() {
        ClientPlayNetworking.send(new OpenPhoneRequestPayload());
    }

    public static void sendBuy(String itemId, int count) {
        ClientPlayNetworking.send(new BuyItemPayload(itemId, count));
    }

    public static void sendBank(boolean deposit, long amount) {
        ClientPlayNetworking.send(new BankActionPayload(deposit, amount));
    }

    public static void syncBalances(ServerPlayerEntity player, MinecraftServer server) {
        var account = com.moderncraft.economy.state.EconomyService.view(server, player.getUuid());
        ServerPlayNetworking.send(player, new BalanceSyncPayload(account.wallet(), account.bank()));
    }

    public static void sendInfo(ServerPlayerEntity player, String msg) {
        ServerPlayNetworking.send(player, new PhoneMessagePayload(msg, false));
    }

    public static void sendError(ServerPlayerEntity player, String msg) {
        ServerPlayNetworking.send(player, new PhoneMessagePayload(msg, true));
    }
}
