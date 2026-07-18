package com.moderncraft.economy.jobs;

import net.minecraft.util.math.BlockPos;

/** A heavy-load delivery assigned to one concrete Moderncraft target. */
public record LoaderOrder(String itemId, int count, long reward, BlockPos destination, long expiresAt) {
    public boolean isValid() {
        return itemId != null && !itemId.isBlank() && count > 0 && reward > 0
                && destination != null && expiresAt > 0;
    }

    public boolean expired(long worldTime) { return worldTime >= expiresAt; }
}
