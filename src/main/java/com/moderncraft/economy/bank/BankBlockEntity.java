package com.moderncraft.economy.bank;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Marker block-entity for a bank branch. Stores one stat:
 * {@code totalDeposits} (lifetime M\$ deposited through this branch).
 * The actual money is held in the per-player PlayerAccount, not here.
 */
public class BankBlockEntity extends BlockEntity {

    private long totalDeposits = 0L;

    public BankBlockEntity(BlockPos pos, BlockState state) {
        super(com.moderncraft.economy.ModBlockEntities.BANK_BE, pos, state);
    }

    public long totalDeposits() { return totalDeposits; }

    public void addDeposit(long amount) {
        this.totalDeposits += amount;
        markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("totalDeposits", this.totalDeposits);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("totalDeposits")) this.totalDeposits = nbt.getLong("totalDeposits");
    }
}
