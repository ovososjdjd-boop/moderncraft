package com.moderncraft.economy.jobs;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** A parcel request issued by the cafe for one concrete resident. */
public record CourierOrder(
        String itemId,
        int count,
        long reward,
        UUID recipientId,
        BlockPos recipientPosition,
        long expiresAt
) {
    public boolean isValid() {
        return itemId != null && !itemId.isBlank()
                && count > 0 && reward > 0
                && recipientId != null && recipientPosition != null
                && expiresAt > 0;
    }

    public boolean expired(long worldTime) { return worldTime >= expiresAt; }
}
