package com.moderncraft.economy.pickup;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Per-chunk state for a placed Pickup Point block. We don't store the items
 * here — sales are atomic (item disappears from the player, money is added
 * immediately), so the block entity is mostly a marker that proves "a Pickup
 * Point is here, and the GUI is allowed to open for this position".
 * <p>
 * We do store one persistent counter: {@code totalSold}. It tracks how many
 * M\$ have passed through this point since it was placed. Useful for stats
 * and for a future "manager" feature.
 */
public class PickupPointBlockEntity extends BlockEntity {

    private long totalSold = 0L;

    public PickupPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PICKUP_POINT_BE, pos, state);
    }

    public long totalSold() { return totalSold; }

    public void addToTotal(long amount) {
        this.totalSold += amount;
        this.markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("totalSold", this.totalSold);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("totalSold")) {
            this.totalSold = nbt.getLong("totalSold");
        }
    }
}
