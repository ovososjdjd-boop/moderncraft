package com.moderncraft.economy.bank;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.state.EconomyService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Packets for the bank building.
 * <ul>
 *     <li>{@code OpenScreenPayload} (S2C) — server opens the bank UI for
 *         this branch, with current balances.</li>
 *     <li>{@code BankDepositPayload} (C2S) — player deposits N M\$.</li>
 *     <li>{@code BankWithdrawPayload} (C2S) — player withdraws N M\$.</li>
 * </ul>
 * The screen is similar to the phone's bank app but rendered as a stand-alone
 * building, and it has a stats line ("this branch has processed X M\$").
 */
public final class BankNetworking {

    public static final Identifier OPEN_ID = Moderncraft.id("bank_open");
    public static final Identifier DEPOSIT_ID = Moderncraft.id("bank_deposit");
    public static final Identifier WITHDRAW_ID = Moderncraft.id("bank_withdraw");

    public record OpenScreenPayload(BlockPos pos, long wallet, long bank, long branchTotal)
            implements CustomPayload {
        public static final CustomPayload.Id<OpenScreenPayload> ID = new CustomPayload.Id<>(OPEN_ID);
        public static final PacketCodec<PacketByteBuf, OpenScreenPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeBlockPos(p.pos);
                            buf.writeLong(p.wallet);
                            buf.writeLong(p.bank);
                            buf.writeLong(p.branchTotal);
                        },
                        buf -> new OpenScreenPayload(
                                buf.readBlockPos(), buf.readLong(), buf.readLong(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BankDepositPayload(BlockPos pos, long amount) implements CustomPayload {
        public static final CustomPayload.Id<BankDepositPayload> ID = new CustomPayload.Id<>(DEPOSIT_ID);
        public static final PacketCodec<PacketByteBuf, BankDepositPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeBlockPos(p.pos);
                            buf.writeLong(p.amount);
                        },
                        buf -> new BankDepositPayload(buf.readBlockPos(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BankWithdrawPayload(BlockPos pos, long amount) implements CustomPayload {
        public static final CustomPayload.Id<BankWithdrawPayload> ID = new CustomPayload.Id<>(WITHDRAW_ID);
        public static final PacketCodec<PacketByteBuf, BankWithdrawPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeBlockPos(p.pos);
                            buf.writeLong(p.amount);
                        },
                        buf -> new BankWithdrawPayload(buf.readBlockPos(), buf.readLong())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BankDepositPayload.ID, BankDepositPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BankWithdrawPayload.ID, BankWithdrawPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(BankDepositPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (payload.amount() <= 0) {
                PhoneNetworking.sendError(player, "Amount must be positive.");
                return;
            }
            EconomyService.Result r = EconomyService.depositToBank(
                    server, player.getUuid(), payload.amount());
            if (r == EconomyService.Result.OK) {
                if (player.getWorld().getBlockEntity(payload.pos()) instanceof BankBlockEntity be) {
                    be.addDeposit(payload.amount());
                }
                PhoneNetworking.sendInfo(player, "Deposited " + payload.amount() + " M$ into the bank.");
            } else {
                PhoneNetworking.sendError(player, errorMessage(r));
            }
            PhoneNetworking.syncBalances(player, server);
            sendOpen(player, payload.pos());
        });

        ServerPlayNetworking.registerGlobalReceiver(BankWithdrawPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (payload.amount() <= 0) {
                PhoneNetworking.sendError(player, "Amount must be positive.");
                return;
            }
            EconomyService.Result r = EconomyService.withdrawFromBank(
                    server, player.getUuid(), payload.amount());
            if (r == EconomyService.Result.OK) {
                PhoneNetworking.sendInfo(player, "Withdrew " + payload.amount() + " M$ from the bank.");
            } else {
                PhoneNetworking.sendError(player, errorMessage(r));
            }
            PhoneNetworking.syncBalances(player, server);
            sendOpen(player, payload.pos());
        });
    }

    private static String errorMessage(EconomyService.Result r) {
        return switch (r) {
            case NOT_ENOUGH_WALLET -> "Not enough money in wallet.";
            case NOT_ENOUGH_BANK -> "Not enough money in bank.";
            case INVALID_AMOUNT -> "Amount must be positive.";
            case PLAYER_NOT_FOUND -> "Player not found.";
            case OK -> "OK.";
        };
    }

    public static void sendOpen(ServerPlayerEntity player, BlockPos pos) {
        var account = EconomyService.view(player.getServer(), player.getUuid());
        long branchTotal = 0L;
        if (player.getWorld().getBlockEntity(pos) instanceof BankBlockEntity be) {
            branchTotal = be.totalDeposits();
        }
        ServerPlayNetworking.send(player, new OpenScreenPayload(
                pos, account.wallet(), account.bank(), branchTotal));
    }

    public static void sendDeposit(BlockPos pos, long amount) {
        ClientPlayNetworking.send(new BankDepositPayload(pos, amount));
    }

    public static void sendWithdraw(BlockPos pos, long amount) {
        ClientPlayNetworking.send(new BankWithdrawPayload(pos, amount));
    }
}
