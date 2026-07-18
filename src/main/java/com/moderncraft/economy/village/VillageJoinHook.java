package com.moderncraft.economy.village;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Detects natural vanilla villages and adds one persistent Moderncraft district to each. */
public final class VillageJoinHook {
    private static final int SCAN_INTERVAL = 40;
    private static int ticks;

    private VillageJoinHook() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> tryGenerateForPlayer(handler.getPlayer())));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % SCAN_INTERVAL != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tryGenerateForPlayer(player);
            }
        });
    }

    public static void tryGenerateForPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (world == null) return;
        BlockPos villageCenter = findNearbyVillage(world, player);
        if (villageCenter == null) return;

        VillageState state = VillageState.get(player.getServer());
        if (state.contains(villageCenter)) return;

        BlockPos districtOrigin = VillageGenerator.findDistrictOrigin(world, villageCenter, player.getBlockPos());
        if (districtOrigin == null) return; // try again when the player returns / chunks load

        VillageGenerator.generate(world, districtOrigin);
        state.markGenerated(villageCenter, districtOrigin);
        player.sendMessage(Text.literal("Moderncraft district added to the village: pickup point, bank, cafe, factory, jobs and stock exchange.")
                .formatted(Formatting.GOLD), true);
    }

    /**
     * Natural villagers are used as a stable, version-independent village signal.
     * Moderncraft residents have AI disabled and are deliberately ignored.
     */
    private static BlockPos findNearbyVillage(ServerWorld world, ServerPlayerEntity player) {
        List<VillagerEntity> nearby = world.getEntitiesByClass(
                VillagerEntity.class,
                player.getBoundingBox().expand(96.0),
                villager -> !villager.isAiDisabled());
        if (nearby.size() < 2) return null;

        VillagerEntity seed = nearby.stream()
                .min((a, b) -> Double.compare(a.squaredDistanceTo(player), b.squaredDistanceTo(player)))
                .orElse(null);
        if (seed == null) return null;

        List<VillagerEntity> cluster = new ArrayList<>();
        for (VillagerEntity villager : nearby) {
            if (villager.squaredDistanceTo(seed) <= 64.0 * 64.0) cluster.add(villager);
        }
        if (cluster.size() < 2) return null;

        double x = cluster.stream().mapToDouble(VillagerEntity::getX).average().orElse(seed.getX());
        double z = cluster.stream().mapToDouble(VillagerEntity::getZ).average().orElse(seed.getZ());
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, (int) x, (int) z);
        return new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
    }
}
