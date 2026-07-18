package com.moderncraft.economy.pickup;

import com.moderncraft.economy.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The "Pickup Point" building — a counterpart to the phone's catalog.
 * <p>
 * Players drop items into it via the GUI; items disappear; the
 * {@code sell} price from the price catalog is credited to the player's wallet.
 * The block is just a marker — the real logic is in {@link PickupPointBlockEntity}
 * and the network packets.
 * <p>
 * We use a regular {@code BlockWithEntity} (not {@code BlockWithEntityRenderer})
 * so the block has a normal cube model. The block is "non-ticking": no
 * per-tick logic, just storage and a GUI entry point.
 */
public class PickupPointBlock extends BlockWithEntity {

    public PickupPointBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PickupPointBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // Default for BlockWithEntity is INVISIBLE; we want the normal model.
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        // Server-side: ask the client to open the pickup point screen.
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            // Open a fresh client screen directly via a payload — we don't use
            // NamedScreenHandlerFactory because we don't have a real Inventory
            // we want the player to interact with (we just list their own
            // inventory items and let them sell via a button).
            com.moderncraft.economy.pickup.PickupPointNetworking.sendOpen(sp, pos);
        }
        return ActionResult.CONSUME;
    }
}
