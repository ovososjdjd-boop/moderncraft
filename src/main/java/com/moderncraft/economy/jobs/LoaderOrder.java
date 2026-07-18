package com.moderncraft.economy.jobs;

/**
 * A loader delivery order — what heavy item the loader wants, how many,
 * and where to deliver it. The destination is a BlockPos in the world.
 */
public record LoaderOrder(String itemId, int count, long reward, net.minecraft.util.math.BlockPos destination) {
    public boolean isValid() {
        return itemId != null && !itemId.isBlank() && count > 0 && reward > 0 && destination != null;
    }
}
