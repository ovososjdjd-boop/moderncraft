package com.moderncraft.economy.state;

/** A paid order waiting at a pickup point. It contains no mutable ItemStack, only a registry id. */
public record PurchaseOrder(String itemId, int count) {
    public boolean valid() { return itemId != null && !itemId.isBlank() && count > 0; }
}
