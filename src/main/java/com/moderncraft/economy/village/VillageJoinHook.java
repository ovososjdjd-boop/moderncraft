package com.moderncraft.economy.village;

import com.moderncraft.Moderncraft;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Wires the village generation into the player join lifecycle.
 * <p>
 * We generate the village on the first player to join a world. We pick a
 * spot near the world spawn (or, if not set, near (0, 64, 0)) and ask the
 * generator to build it.
 * <p>
 * If a second player joins the same world later, we don't generate again —
 * the village is one per world.
 */
public final class VillageJoinHook {

    private VillageJoinHook() {}

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Defer to next tick — at JOIN the world might still be loading.
            server.execute(() -> tryGenerateForPlayer(player));
        });
    }

    public static void tryGenerateForPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (world.isClient) return;
        VillageState state = VillageState.get(player.getServer());
        if (state.isGenerated()) return;

        // Try to find a flat surface near the world spawn.
        BlockPos spawn = world.getSpawnPos();
        if (spawn.getY() <= world.getBottomY()) {
            // No valid spawn yet — bail; we'll try again next join.
            return;
        }
        BlockPos center = findFlatGround(world, spawn);
        if (center == null) {
            Moderncraft.LOGGER.warn("Could not find flat ground for village near {}", spawn);
            return;
        }
        Moderncraft.LOGGER.info("Generating moderncraft village at {}", center);
        VillageGenerator.generate(world, center);
        state.setCenter(center);
        // Welcome the player with a short tour.
        player.sendMessage(net.minecraft.text.Text.literal(""), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "Welcome to your moderncraft village.").formatted(net.minecraft.util.Formatting.GOLD), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "The cafe courier and loader have jobs for you, and residents accept courier parcels. The bank stores your money. The stock exchange buys and sells shares."), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "Right-click the phone in your inventory to open the catalog. /moderncraft for a status summary."), false);
    }

    /**
     * Scans a 50x50 area around the spawn and finds a 5x5 patch of
     * solid stone/grass at the same Y. Used to put the village on a nice
     * flat place instead of half-floating in the air.
     */
    private static BlockPos findFlatGround(ServerWorld world, BlockPos around) {
        int baseY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, around.getX(), around.getZ());
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int r = 0; r <= 30; r += 5) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    int x = around.getX() + dx;
                    int z = around.getZ() + dz;
                    int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
                    if (y <= world.getBottomY() + 4) continue;
                    if (isFlatPatch(world, x, y, z, 3)) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isFlatPatch(ServerWorld world, int cx, int cy, int cz, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, cx + dx, cz + dz);
                if (Math.abs(y - cy) > 2) return false;
            }
        }
        return true;
    }
}
