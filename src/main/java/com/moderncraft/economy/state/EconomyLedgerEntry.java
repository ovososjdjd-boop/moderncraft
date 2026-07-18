package com.moderncraft.economy.state;

/** Compact persistent economy journal entry shown in the player's History screen. */
public record EconomyLedgerEntry(String type, long amount, long timestamp, String note) {
    public EconomyLedgerEntry {
        if (type == null || type.isBlank()) type = "unknown";
        if (note == null) note = "";
    }
}
