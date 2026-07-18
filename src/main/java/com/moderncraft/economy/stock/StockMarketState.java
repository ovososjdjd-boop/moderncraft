package com.moderncraft.economy.stock;

import com.moderncraft.Moderncraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Per-world stock market state. Holds a list of "companies" with a name,
 * a current price (in M\$ per share), a small daily-drift amount and the
 * player's shareholding. The market ticks on a server timer.
 * <p>
 * This is a <b>skeleton</b> — the goal for v1 is to have working
 * buy/sell commands and a screen, with a believable (if not deep) model
 * behind it. Daily drift is the only mechanic for now.
 */
public final class StockMarketState extends PersistentState {

    private static final Identifier ID = Identifier.of(Moderncraft.MOD_ID, "stock_market");

    public static final PersistentStateType<StockMarketState> TYPE = new PersistentStateType<>(
            ID,
            StockMarketState::new,
            StockMarketState::fromNbt,
            null
    );

    /** One company on the exchange. */
    public record Company(String id, String displayName, long price, int drift) {
        public Company withPrice(long newPrice) {
            return new Company(id, displayName, newPrice, drift);
        }
    }

    /** Player shareholding: player UUID -> company id -> share count. */
    private final List<Company> companies = new ArrayList<>();
    private final java.util.Map<java.util.UUID, java.util.Map<String, Integer>> shares = new java.util.HashMap<>();
    private long tickCount = 0L;

    public StockMarketState() {
        seed();
    }

    private void seed() {
        // Five starter companies with names that fit the mod's economy theme.
        companies.add(new Company("logistics",  "Moderncraft Logistics",  1200L, 30));
        companies.add(new Company("redstone",   "RedstoneCo",             800L,  40));
        companies.add(new Company("pickup",     "Pickup Express",         500L,  20));
        companies.add(new Company("bank",       "First M\$ Bank",         1500L, 10));
        companies.add(new Company("factory",    "Industrial Foundry",     2200L, 60));
    }

    public static StockMarketState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        StockMarketState s = new StockMarketState();
        s.tickCount = nbt.getLong("ticks");
        if (nbt.contains("companies")) {
            s.companies.clear();
            NbtCompound c = nbt.getCompound("companies");
            for (String key : c.getKeys()) {
                NbtCompound row = c.getCompound(key);
                s.companies.add(new Company(
                        key,
                        row.getString("name"),
                        row.getLong("price"),
                        row.getInt("drift")
                ));
            }
        }
        if (nbt.contains("shares")) {
            NbtCompound all = nbt.getCompound("shares");
            for (String uuidKey : all.getKeys()) {
                try {
                    java.util.UUID playerId = java.util.UUID.fromString(uuidKey);
                    NbtCompound portfolio = all.getCompound(uuidKey);
                    java.util.Map<String, Integer> owned = new java.util.HashMap<>();
                    for (String companyId : portfolio.getKeys()) {
                        int count = portfolio.getInt(companyId);
                        if (count > 0) owned.put(companyId, count);
                    }
                    if (!owned.isEmpty()) s.shares.put(playerId, owned);
                } catch (IllegalArgumentException ignored) {
                    // Ignore the pre-0.2 global-share format rather than assigning
                    // another player's shares to the first player who joins.
                }
            }
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("ticks", tickCount);
        NbtCompound c = new NbtCompound();
        for (Company comp : companies) {
            NbtCompound row = new NbtCompound();
            row.putString("name", comp.displayName());
            row.putLong("price", comp.price());
            row.putInt("drift", comp.drift());
            c.put(comp.id(), row);
        }
        nbt.put("companies", c);
        NbtCompound sh = new NbtCompound();
        for (var portfolio : shares.entrySet()) {
            NbtCompound owned = new NbtCompound();
            for (var holding : portfolio.getValue().entrySet()) {
                if (holding.getValue() > 0) owned.putInt(holding.getKey(), holding.getValue());
            }
            if (!owned.isEmpty()) sh.put(portfolio.getKey().toString(), owned);
        }
        nbt.put("shares", sh);
        return nbt;
    }

    public static StockMarketState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) throw new IllegalStateException("overworld not loaded");
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public List<Company> companies() { return companies; }

    public Company byId(String id) {
        for (Company c : companies) if (c.id().equals(id)) return c;
        return null;
    }

    public int sharesOf(java.util.UUID playerId, String id) {
        return shares.getOrDefault(playerId, java.util.Map.of()).getOrDefault(id, 0);
    }

    public int totalShares(java.util.UUID playerId) {
        return shares.getOrDefault(playerId, java.util.Map.of()).values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    public boolean buy(java.util.UUID playerId, String id, int qty) {
        if (qty <= 0) return false;
        Company c = byId(id);
        if (c == null) return false;
        java.util.Map<String, Integer> portfolio = shares.computeIfAbsent(playerId, ignored -> new java.util.HashMap<>());
        int current = portfolio.getOrDefault(id, 0);
        if (current > Integer.MAX_VALUE - qty) return false;
        portfolio.put(id, current + qty);
        shiftPrice(id, Math.max(1L, c.price() / 1000L));
        markDirty();
        return true;
    }

    public long sell(java.util.UUID playerId, String id, int qty) {
        if (qty <= 0) return 0L;
        Company c = byId(id);
        if (c == null) return 0L;
        java.util.Map<String, Integer> portfolio = shares.get(playerId);
        if (portfolio == null) return 0L;
        int have = portfolio.getOrDefault(id, 0);
        int actual = Math.min(qty, have);
        if (actual == 0) return 0L;
        if (actual == have) portfolio.remove(id);
        else portfolio.put(id, have - actual);
        if (portfolio.isEmpty()) shares.remove(playerId);
        shiftPrice(id, -Math.max(1L, c.price() / 1000L));
        markDirty();
        return (long) actual * c.price();
    }

    private void shiftPrice(String id, long delta) {
        Company current = byId(id);
        if (current == null) return;
        long next = Math.max(50L, current.price() + delta);
        for (int i = 0; i < companies.size(); i++) {
            if (companies.get(i).id().equals(id)) {
                companies.set(i, current.withPrice(next));
                return;
            }
        }
    }

    /**
     * Server-side daily tick: every 20 minutes, drift each company's price
     * randomly within ±drift, clamp to a sane minimum. Skeleton model — the
     * real version would react to in-game events.
     */
    public void tick(Random random) {
        tickCount++;
        if (tickCount % 24000L != 0) return; // 24000 ticks = 20 minutes
        List<Company> next = new ArrayList<>();
        for (Company c : companies) {
            int delta = random.nextInt(c.drift() * 2 + 1) - c.drift();
            long newPrice = c.price() + delta;
            if (newPrice < 50L) newPrice = 50L;
            next.add(c.withPrice(newPrice));
        }
        companies.clear();
        companies.addAll(next);
        markDirty();
        Moderncraft.LOGGER.info("[stock-market] daily tick applied to {} companies", companies.size());
    }
}
