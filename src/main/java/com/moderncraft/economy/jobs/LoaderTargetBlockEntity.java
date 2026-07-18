package com.moderncraft.economy.jobs;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Marker block-entity for a loader delivery point. */
public class LoaderTargetBlockEntity extends BlockEntity {
    public LoaderTargetBlockEntity(BlockPos pos, BlockState state) {
        super(com.moderncraft.economy.ModBlockEntities.LOADER_TARGET_BE, pos, state);
    }
}
