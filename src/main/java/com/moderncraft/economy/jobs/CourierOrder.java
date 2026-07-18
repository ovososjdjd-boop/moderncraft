package com.moderncraft.economy.jobs;

/**
 * A single courier order — what the player needs to bring and how much
 * they get paid. Created server-side when the courier generates a new
 * request, and synchronised to the client so the highlight and any UI
 * show consistent data.
 *
 * @param itemId   namespaced item id, e.g. {@code minecraft:diamond}
 * @param count    how many the courier wants
 * @param reward   M\$ the courier pays for the delivery
 */
public record CourierOrder(String itemId, int count, long reward) {

    public boolean isValid() {
        return itemId != null && !itemId.isBlank() && count > 0 && reward > 0;
    }
}
