package com.moderncraft.economy.state;

import com.moderncraft.Moderncraft;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side economy state, saved with the world.
 * <p>
 * Holds a per-player {@link PlayerAccount} keyed by UUID. We don't subclass
 * {@code PersistentState} with a custom NBT codec; instead we use Mojang's
 * {@code Codec} (the modern, supported way in 1.21+) which serialises to NBT
 * automatically and keeps the code much shorter.
 * <p>
 * The same instance is reused across ticks — we mutate the inner map, then
 * call {@link #markDirty()} so the file is flushed on world save.
 */
public final class WorldEconomyState extends PersistentState {

    /** Codec-of-Map wrapper, not directly a Codec&lt;WorldEconomyState&gt; for clarity. */
    private static final Codec<Map<UUID, PlayerAccount>> ACCOUNTS_CODEC =
            Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString),
                               PlayerAccount.CODEC);

    private final Map<UUID, PlayerAccount> accounts = new HashMap<>();
    private final Map<UUID, List<PurchaseOrder>> pendingOrders = new HashMap<>();

    public static final PersistentStateType<WorldEconomyState> TYPE = new PersistentStateType<>(
            Identifier.of(Moderncraft.MOD_ID, "world_economy"),
            WorldEconomyState::new,
            WorldEconomyState::fromNbt,
            null
    );

    public WorldEconomyState() {}

    /**
     * Player state accessor — creates an empty account on first read.
     * <p>
     * Note: this does NOT mark dirty. Callers that mutate the result must call
     * {@code state.markDirty()} themselves (or use {@link EconomyService} which does it for them).
     */
    public PlayerAccount getOrCreate(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> PlayerAccount.EMPTY);
    }

    /**
     * Replace the stored account for a player. Marks the state dirty.
     */
    public void put(UUID playerId, PlayerAccount account) {
        accounts.put(playerId, account);
        markDirty();
    }

    public Map<UUID, PlayerAccount> all() {
        return accounts;
    }

    public List<PurchaseOrder> pendingOrders(UUID playerId) {
        return List.copyOf(pendingOrders.getOrDefault(playerId, List.of()));
    }

    public boolean canAcceptOrder(UUID playerId, int additionalCount) {
        if (additionalCount <= 0) return false;
        long current = pendingOrders(playerId).stream().mapToLong(PurchaseOrder::count).sum();
        return current + additionalCount <= 100_000L;
    }

    public void addOrder(UUID playerId, PurchaseOrder order) {
        if (!order.valid() || !canAcceptOrder(playerId, order.count())) {
            throw new IllegalArgumentException("Invalid or oversized purchase order");
        }
        List<PurchaseOrder> orders = pendingOrders.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        for (int i = 0; i < orders.size(); i++) {
            PurchaseOrder existing = orders.get(i);
            if (existing.itemId().equals(order.itemId())) {
                orders.set(i, new PurchaseOrder(existing.itemId(), existing.count() + order.count()));
                markDirty();
                return;
            }
        }
        orders.add(order);
        markDirty();
    }

    /** Removes and returns all paid orders atomically; called only at a pickup point. */
    public List<PurchaseOrder> takeOrders(UUID playerId) {
        List<PurchaseOrder> orders = pendingOrders.remove(playerId);
        markDirty();
        return orders == null ? List.of() : List.copyOf(orders);
    }

    // --- persistence plumbing ------------------------------------------------

    public static WorldEconomyState fromNbt(net.minecraft.nbt.NbtCompound nbt,
                                            net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        WorldEconomyState state = new WorldEconomyState();
        if (nbt.contains("accounts")) {
            // Decode the inner Map directly from the NBT compound key.
            net.minecraft.nbt.NbtCompound inner = nbt.getCompound("accounts");
            for (String key : inner.getKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    PlayerAccount account = PlayerAccount.CODEC.parse(
                            net.minecraft.nbt.NbtOps.INSTANCE,
                            inner.getCompound(key)
                    ).getOrThrow();
                    state.accounts.put(id, account);
                } catch (Exception e) {
                    Moderncraft.LOGGER.warn("Skipping corrupt account for {}: {}", key, e.toString());
                }
            }
        }
        if (nbt.contains("pendingOrders")) {
            net.minecraft.nbt.NbtCompound orders = nbt.getCompound("pendingOrders");
            for (String key : orders.getKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    net.minecraft.nbt.NbtList list = orders.getList(key, net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
                    List<PurchaseOrder> parsed = new ArrayList<>();
                    for (net.minecraft.nbt.NbtElement element : list) {
                        net.minecraft.nbt.NbtCompound order = (net.minecraft.nbt.NbtCompound) element;
                        PurchaseOrder parsedOrder = new PurchaseOrder(order.getString("item"), order.getInt("count"));
                        if (parsedOrder.valid()) parsed.add(parsedOrder);
                    }
                    if (!parsed.isEmpty()) state.pendingOrders.put(id, parsed);
                } catch (Exception e) {
                    Moderncraft.LOGGER.warn("Skipping corrupt pending orders for {}: {}", key, e.toString());
                }
            }
        }
        return state;
    }

    @Override
    public net.minecraft.nbt.NbtCompound writeNbt(net.minecraft.nbt.NbtCompound nbt) {
        net.minecraft.nbt.NbtCompound inner = new net.minecraft.nbt.NbtCompound();
        for (Map.Entry<UUID, PlayerAccount> entry : accounts.entrySet()) {
            net.minecraft.nbt.NbtElement encoded = PlayerAccount.CODEC.encodeStart(
                    net.minecraft.nbt.NbtOps.INSTANCE, entry.getValue()
            ).getOrThrow();
            inner.put(entry.getKey().toString(), encoded);
        }
        nbt.put("accounts", inner);
        net.minecraft.nbt.NbtCompound orders = new net.minecraft.nbt.NbtCompound();
        for (Map.Entry<UUID, List<PurchaseOrder>> entry : pendingOrders.entrySet()) {
            net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
            for (PurchaseOrder order : entry.getValue()) {
                net.minecraft.nbt.NbtCompound encoded = new net.minecraft.nbt.NbtCompound();
                encoded.putString("item", order.itemId());
                encoded.putInt("count", order.count());
                list.add(encoded);
            }
            if (!list.isEmpty()) orders.put(entry.getKey().toString(), list);
        }
        nbt.put("pendingOrders", orders);
        return nbt;
    }

    /** Single entry point used by all gameplay code. */
    public static WorldEconomyState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded — cannot access economy state");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }
}
