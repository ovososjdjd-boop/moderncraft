package com.moderncraft.economy.factory;

import com.moderncraft.economy.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Factory's main block. Has the working BlockEntity, opens the GUI.
 * <p>
 * When the player right-clicks and the factory is well-formed (chimney on
 * top, pipe on the side, gear above), we open the working screen and start
 * tracking a "shift" for the player. Money is paid by the worker NPC when
 * the player finishes the shift; this block just runs the production timer.
 */
public class FactoryBaseBlock extends BlockWithEntity {

    public FactoryBaseBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public <T extends BlockEntity> net.minecraft.block.entity.BlockEntityTicker<T> getTicker(
            World world, BlockState state, net.minecraft.block.entity.BlockEntityType<T> type) {
        if (world.isClient) return null;
        return type == com.moderncraft.economy.ModBlockEntities.FACTORY_BE
                ? (w, p, s, be) -> FactoryBlockEntity.serverTick(w, p, s, (FactoryBlockEntity) be)
                : null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && world.getBlockEntity(pos) instanceof FactoryBlockEntity be) {
            be.markStructureDirty();
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof FactoryBlockEntity be)) {
            return ActionResult.PASS;
        }
        be.markStructureDirty();
        if (!be.isStructureComplete()) {
            player.sendMessage(Text.literal(
                    "The factory is missing parts. Build chimney on top, pipe on the side, gear above the base."),
                    true);
            return ActionResult.CONSUME;
        }
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            com.moderncraft.economy.factory.FactoryNetworking.sendOpen(sp, pos);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                               BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && world.getBlockEntity(pos) instanceof FactoryBlockEntity be) {
            be.markStructureDirty();
        }
    }
}
