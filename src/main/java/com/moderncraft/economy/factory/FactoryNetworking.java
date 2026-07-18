package com.moderncraft.economy.factory;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.state.EconomyService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Network packets for the factory flow.
 * <ul>
 *     <li>{@code OpenScreenPayload} (S2C) — server says "open the factory UI".</li>
 *     <li>{@code StartShiftPayload} (C2S) — player wants to start a shift here.</li>
 *     <li>{@code EndShiftPayload} (C2S) — player is ending the shift early; we
 *         pay them based on items produced, give them the items, and notify
 *         via toast + balance sync.</li>
 *     <li>{@code FactoryStatePayload} (S2C) — server pushes current progress
 *         so the UI can re-render (called periodically and on screen open).</li>
 * </ul>
 */
public final class FactoryNetworking {

    private static boolean commonRegistered = false;

    public static final Identifier OPEN_ID = Moderncraft.id("factory_open");
    public static final Identifier START_ID = Moderncraft.id("factory_start");
    public static final Identifier END_ID = Moderncraft.id("factory_end");
    public static final Identifier STATE_ID = Moderncraft.id("factory_state");

    public record OpenScreenPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<OpenScreenPayload> ID = new CustomPayload.Id<>(OPEN_ID);
        public static final PacketCodec<PacketByteBuf, OpenScreenPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> buf.writeBlockPos(p.pos),
                        buf -> new OpenScreenPayload(buf.readBlockPos())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record StartShiftPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<StartShiftPayload> ID = new CustomPayload.Id<>(START_ID);
        public static final PacketCodec<PacketByteBuf, StartShiftPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> buf.writeBlockPos(p.pos),
                        buf -> new StartShiftPayload(buf.readBlockPos())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record EndShiftPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<EndShiftPayload> ID = new CustomPayload.Id<>(END_ID);
        public static final PacketCodec<PacketByteBuf, EndShiftPayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> buf.writeBlockPos(p.pos),
                        buf -> new EndShiftPayload(buf.readBlockPos())
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FactoryStatePayload(
            BlockPos pos,
            boolean structureValid,
            boolean hasOwner,
            boolean isYouOwner,
            int shiftTicks,
            int shiftDuration,
            List<String> producedIds,
            List<Integer> producedCounts
    ) implements CustomPayload {
        public static final CustomPayload.Id<FactoryStatePayload> ID = new CustomPayload.Id<>(STATE_ID);
        public static final PacketCodec<PacketByteBuf, FactoryStatePayload> CODEC =
                PacketCodec.of(
                        (p, buf) -> {
                            buf.writeBlockPos(p.pos);
                            buf.writeBoolean(p.structureValid);
                            buf.writeBoolean(p.hasOwner);
                            buf.writeBoolean(p.isYouOwner);
                            buf.writeInt(p.shiftTicks);
                            buf.writeInt(p.shiftDuration);
                            buf.writeInt(p.producedIds.size());
                            for (int i = 0; i < p.producedIds.size(); i++) {
                                buf.writeString(p.producedIds.get(i));
                                buf.writeInt(p.producedCounts.get(i));
                            }
                        },
                        buf -> {
                            BlockPos pos = buf.readBlockPos();
                            boolean sv = buf.readBoolean();
                            boolean ho = buf.readBoolean();
                            boolean iyo = buf.readBoolean();
                            int st = buf.readInt();
                            int sd = buf.readInt();
                            int n = buf.readInt();
                            List<String> ids = new ArrayList<>(n);
                            List<Integer> counts = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                ids.add(buf.readString());
                                counts.add(buf.readInt());
                            }
                            return new FactoryStatePayload(pos, sv, ho, iyo, st, sd, ids, counts);
                        }
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerCommon() {
        if (commonRegistered) return;
        commonRegistered = true;
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FactoryStatePayload.ID, FactoryStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartShiftPayload.ID, StartShiftPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EndShiftPayload.ID, EndShiftPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(StartShiftPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            if (!(player.getWorld().getBlockEntity(payload.pos()) instanceof FactoryBlockEntity be)) return;
            if (!be.isStructureComplete()) {
                PhoneNetworking.sendError(player, "Factory is incomplete.");
                return;
            }
            if (be.hasOwner()) {
                if (be.isOwner(player.getUuid())) {
                    PhoneNetworking.sendInfo(player, "Your shift is already running.");
                } else {
                    PhoneNetworking.sendError(player, "Someone else is already working this shift.");
                }
                sendState(player, be);
                return;
            }
            be.startShift(player.getUuid());
            PhoneNetworking.sendInfo(player, "Shift started. Work for 3 minutes to produce parts.");
            sendState(player, be);
        });

        ServerPlayNetworking.registerGlobalReceiver(EndShiftPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = player.getServer();
            if (!(player.getWorld().getBlockEntity(payload.pos()) instanceof FactoryBlockEntity be)) return;
            if (!be.isOwner(player.getUuid())) {
                PhoneNetworking.sendError(player, "You aren't running this shift.");
                sendState(player, be);
                return;
            }
            // Compute pay: 50 M\$ per produced item, plus a small completion bonus.
            List<ItemStack> output = be.endShift();
            int producedCount = output.stream().mapToInt(ItemStack::getCount).sum();
            long pay = (long) producedCount * 50L + 100L;
            EconomyService.creditWallet(server, player.getUuid(), pay);

            // Try to give the produced items to the player.
            int undelivered = 0;
            for (ItemStack stack : output) {
                if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) {
                    undelivered += stack.getCount();
                    // Never delete manufactured goods: drop the remainder at the
                    // player's feet so a full inventory is not an economic loss.
                    player.dropStack(stack);
                }
            }
            if (undelivered > 0) {
                PhoneNetworking.sendError(player, "Your inventory was full; "
                        + undelivered + " items were dropped at your feet.");
            }
            PhoneNetworking.sendInfo(player, "Shift complete. Earned " + pay + " M\$.");
            PhoneNetworking.syncBalances(player, server);
            sendState(player, be);
        });
    }

    public static void sendOpen(ServerPlayerEntity player, BlockPos pos) {
        ServerPlayNetworking.send(player, new OpenScreenPayload(pos));
        if (player.getWorld().getBlockEntity(pos) instanceof FactoryBlockEntity be) {
            sendState(player, be);
        }
    }

    public static void sendState(ServerPlayerEntity player, FactoryBlockEntity be) {
        List<String> ids = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ItemStack s : be.produced()) {
            ids.add(net.minecraft.registry.Registries.ITEM.getId(s.getItem()).toString());
            counts.add(s.getCount());
        }
        ServerPlayNetworking.send(player, new FactoryStatePayload(
                be.getPos(),
                be.isStructureComplete(),
                be.hasOwner(),
                be.isOwner(player.getUuid()),
                be.shiftTicks(),
                be.shiftDuration(),
                ids,
                counts
        ));
    }

    public static void sendStart(BlockPos pos) {
        ClientPlayNetworking.send(new StartShiftPayload(pos));
    }

    public static void sendEnd(BlockPos pos) {
        ClientPlayNetworking.send(new EndShiftPayload(pos));
    }
}
