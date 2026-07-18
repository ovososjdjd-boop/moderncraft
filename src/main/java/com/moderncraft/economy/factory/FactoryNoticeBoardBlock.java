package com.moderncraft.economy.factory;

import com.moderncraft.economy.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Factory Worker's "notice board" — a small decorative block that acts
 * as the job issuer for a Factory building. Right-clicking the board opens
 * the Factory UI of the nearest complete factory.
 * <p>
 * Why a block instead of an NPC entity? Custom entities in 1.21 require a
 * model, a renderer, AI, animations and a texture — a lot of work for a v1
 * job issuer. The notice board serves the same narrative role ("a job
 * poster at the factory") with a tenth of the code, and we can swap it for
 * a real entity later without touching the rest of the factory logic.
 */
public class FactoryNoticeBoardBlock extends BlockWithEntity {

    public FactoryNoticeBoardBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryNoticeBoardBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        // Find the closest complete factory within 12 blocks.
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (world.getBlockState(cursor).getBlock() instanceof FactoryBaseBlock) {
                        double d = cursor.getSquaredDistance(pos);
                        if (d < bestDist) {
                            bestDist = d;
                            best = cursor.toImmutable();
                        }
                    }
                }
            }
        }
        if (best == null) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "There's no factory nearby. Build one first."), true);
            return ActionResult.CONSUME;
        }
        if (world.getBlockEntity(best) instanceof FactoryBlockEntity be && !be.isStructureComplete()) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "The factory is missing parts. Build chimney on top, pipe on the side, gear above the base."), true);
            return ActionResult.CONSUME;
        }
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            FactoryNetworking.sendOpen(sp, best);
        }
        return ActionResult.CONSUME;
    }
}
