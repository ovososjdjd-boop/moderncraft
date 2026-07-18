package com.moderncraft.economy.stock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Marker block-entity for the stock exchange. State lives in StockMarketState. */
public class StockExchangeBlockEntity extends BlockEntity {
    public StockExchangeBlockEntity(BlockPos pos, BlockState state) {
        super(com.moderncraft.economy.ModBlockEntities.STOCK_EXCHANGE_BE, pos, state);
    }
}
