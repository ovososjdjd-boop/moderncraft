package com.moderncraft.economy.stock;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.state.EconomyService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Packets for the stock market screen.
 * <ul>
 *     <li>{@code OpenScreenPayload} (S2C) — server pushes the full market
 *         snapshot (companies, prices, player's shares, wallet) to the
 *         client.</li>
 *     <li>{@code BuySharesPayload} (C2S) — buy N shares of a company.</li>
 *     <li>{@code SellSharesPayload} (C2S) — sell N shares.</li>
 * </ul>
 * A daily tick is also scheduled via {@code ServerTickEvents} so prices
 * drift on their own (very simple random walk for v1).
 */
public final class StockNetworking {

    public static final Identifier OPEN_ID = Moderncraft.id("stock_open");
    public static final Identifier BUY_ID = Moderncraft.id("stock_buy");
    public static final Identifier SELL_ID = Moderncraft.id("stock_sell");

    public record OpenScreenPayload(
            List<String> companyIds,
            List<String> companyNames,
            List<Long> companyPrices,
            List<Integer> shareCounts,
            long wallet
    ) implements CustomPayload {
        public static final CustomPayload.Id<OpenScreenPayload> ID = new CustomPayload.Id<>(OPEN_ID);
        public static final PacketCodec<PacketByteBuf, OpenScreenPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeInt(p.companyIds.size());
                            for (int i = 0; i < p.companyIds.size(); i++) {
                                buf.writeString(p.companyIds.get(i));
                                buf.writeString(p.companyNames.get(i));
                                buf.writeLong(p.companyPrices.get(i));
                                buf.writeInt(p.shareCounts.get(i));
                            }
                            buf.writeLong(p.wallet);
                        },
                        buf -> {
                            int n = buf.readInt();
                            List<String> ids = new ArrayList<>(n);
                            List<String> names = new ArrayList<>(n);
                            List<Long> prices = new ArrayList<>(n);
                            List<Integer> shares = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                ids.add(buf.readString());
                                names.add(buf.readString());
                                prices.add(buf.readLong());
                                shares.add(buf.readInt());
                            }
                            return new OpenScreenPayload(ids, names, prices, shares, buf.readLong());
                        }
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BuySharesPayload(String companyId, int count) implements CustomPayload {
        public static final CustomPayload.Id<BuySharesPayload> ID = new CustomPayload.Id<>(BUY_ID);
        public static final PacketCodec<PacketByteBuf, BuySharesPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeString(p.companyId);
                            buf.writeInt(p.count);
                        },
                        buf -> new BuySharesPayload(buf.readString(), buf.readInt())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SellSharesPayload(String companyId, int count) implements CustomPayload {
        public static final CustomPayload.Id<SellSharesPayload> ID = new CustomPayload.Id<>(SELL_ID);
        public static final PacketCodec<PacketByteBuf, SellSharesPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeString(p.companyId);
                            buf.writeInt(p.count);
                        },
                        buf -> new SellSharesPayload(buf.readString(), buf.readInt())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuySharesPayload.ID, BuySharesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SellSharesPayload.ID, SellSharesPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(BuySharesPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (payload.count() <= 0) {
                PhoneNetworking.sendError(player, "Count must be positive.");
                return;
            }
            StockMarketState m = StockMarketState.get(server);
            var c = m.byId(payload.companyId());
            if (c == null) {
                PhoneNetworking.sendError(player, "Unknown company: " + payload.companyId());
                return;
            }
            long cost = c.price() * payload.count();
            EconomyService.Result r = EconomyService.debitWallet(server, player.getUuid(), cost);
            if (r != EconomyService.Result.OK) {
                PhoneNetworking.sendError(player, switch (r) {
                    case NOT_ENOUGH_WALLET -> "Not enough money. Need " + cost + " M$.";
                    default -> "Buy failed.";
                });
                sendOpen(player);
                return;
            }
            m.buy(payload.companyId(), payload.count());
            PhoneNetworking.sendInfo(player, "Bought " + payload.count() + " shares of "
                    + c.displayName() + " for " + cost + " M$.");
            PhoneNetworking.syncBalances(player, server);
            sendOpen(player);
        });

        ServerPlayNetworking.registerGlobalReceiver(SellSharesPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (payload.count() <= 0) {
                PhoneNetworking.sendError(player, "Count must be positive.");
                return;
            }
            StockMarketState m = StockMarketState.get(server);
            var c = m.byId(payload.companyId());
            if (c == null) {
                PhoneNetworking.sendError(player, "Unknown company: " + payload.companyId());
                return;
            }
            long proceeds = m.sell(payload.companyId(), payload.count());
            if (proceeds == 0L) {
                PhoneNetworking.sendError(player, "You don't own any shares of " + c.displayName() + ".");
                sendOpen(player);
                return;
            }
            EconomyService.creditWallet(server, player.getUuid(), proceeds);
            PhoneNetworking.sendInfo(player, "Sold shares of " + c.displayName()
                    + " for " + proceeds + " M$.");
            PhoneNetworking.syncBalances(player, server);
            sendOpen(player);
        });

        // Daily tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var world : server.getWorlds()) {
                if (world.getRegistryKey() != net.minecraft.world.World.OVERWORLD) continue;
                StockMarketState m = StockMarketState.get(server);
                m.tick(new java.util.Random());
            }
        });
    }

    public static void sendOpen(ServerPlayerEntity player) {
        var server = player.getServer();
        StockMarketState m = StockMarketState.get(server);
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Long> prices = new ArrayList<>();
        List<Integer> shares = new ArrayList<>();
        for (var c : m.companies()) {
            ids.add(c.id());
            names.add(c.displayName());
            prices.add(c.price());
            shares.add(m.sharesOf(c.id()));
        }
        var account = EconomyService.view(server, player.getUuid());
        ServerPlayNetworking.send(player, new OpenScreenPayload(
                ids, names, prices, shares, account.wallet()));
    }

    public static void sendBuy(String id, int count) {
        ClientPlayNetworking.send(new BuySharesPayload(id, count));
    }

    public static void sendSell(String id, int count) {
        ClientPlayNetworking.send(new SellSharesPayload(id, count));
    }
}
