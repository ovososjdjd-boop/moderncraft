package com.moderncraft.economy.stock;

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
 * The stock exchange building. Right-click opens the stock market screen.
 * No tile-entity state of its own — all the data lives in
 * {@link StockMarketState}, which is per-world.
 */
public class StockExchangeBlock extends BlockWithEntity {

    public StockExchangeBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StockExchangeBlockEntity(pos, state);
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
            com.moderncraft.economy.stock.StockNetworking.sendOpen(sp);
        }
        return ActionResult.CONSUME;
    }
}
