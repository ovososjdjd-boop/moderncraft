package com.moderncraft.economy.jobs;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Marker block-entity for a job-board block. Carries the role string.
 * <p>
 * The actual BlockEntityType is passed in because we have two separate
 * types (CAFE_BOARD_BE and LOADER_BOARD_BE) for the two roles.
 */
public class JobBoardBlockEntity extends BlockEntity {

    private final String role;

    public JobBoardBlockEntity(BlockEntityType<?> type, String role, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.role = role;
    }

    public String role() { return role; }
}
