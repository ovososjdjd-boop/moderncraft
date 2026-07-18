package com.moderncraft.economy.bank;

import com.moderncraft.economy.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
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
 * The bank building's main block. Has a tile entity that records the total
 * deposits made through this branch (a stat — every bank branch keeps its
 * own counter). Right-click opens the bank UI.
 * <p>
 * The bank screen is the same flow we already have on the phone — deposit
 * and withdraw from your bank balance — just presented as a stand-alone
 * building so the player has a reason to visit a physical location.
 */
public class BankBlock extends BlockWithEntity {

    public BankBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BankBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            com.moderncraft.economy.bank.BankNetworking.sendOpen(sp, pos);
        }
        return ActionResult.CONSUME;
    }
}
