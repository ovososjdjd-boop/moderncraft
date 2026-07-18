package com.moderncraft.economy.factory;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Marker block-entity for the notice board. We don't store any state — the
 * block is just a positional anchor and a "this is a job issuer" signal.
 * Kept as a BlockEntity for future expansion (custom text, hired workers,
 * schedules...).
 */
public class FactoryNoticeBoardBlockEntity extends BlockEntity {
    public FactoryNoticeBoardBlockEntity(BlockPos pos, BlockState state) {
        super(com.moderncraft.economy.ModBlockEntities.FACTORY_NOTICE_BOARD_BE, pos, state);
    }
}
