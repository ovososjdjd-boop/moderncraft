package com.moderncraft.economy.factory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Decorative gear that sits on top of the factory base. Has 4 visual rotation
 * states; we cycle through them from the BlockEntity via {@code scheduledTick}
 * while a shift is active. Pure cosmetic.
 */
public class FactoryGearBlock extends Block {

    public static final IntProperty ROTATION = IntProperty.of("rotation", 0, 3);

    public FactoryGearBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ROTATION, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    /**
     * Updates the visual rotation if the block is currently a {@link FactoryGearBlock}
     * and the position matches.
     */
    public static void tickRotation(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof FactoryGearBlock)) return;
        int next = (state.get(ROTATION) + 1) & 3;
        world.setBlockState(pos, state.with(ROTATION, next));
    }
}
