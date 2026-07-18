package com.moderncraft.economy.village;

import com.moderncraft.Moderncraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent registry of natural villages that already received a Moderncraft district. */
public final class VillageState extends PersistentState {
    private static final Identifier ID = Identifier.of(Moderncraft.MOD_ID, "village");
    private static final PersistentStateType<VillageState> TYPE = new PersistentStateType<>(
            ID, VillageState::new, VillageState::fromNbt, null);

    /** village key -> generated district origin */
    private final Map<String, BlockPos> districts = new LinkedHashMap<>();

    public VillageState() {}

    public static VillageState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        VillageState state = new VillageState();
        if (nbt.contains("districts")) {
            NbtCompound stored = nbt.getCompound("districts");
            for (String key : stored.getKeys()) {
                NbtCompound pos = stored.getCompound(key);
                state.districts.put(key, new BlockPos(pos.getInt("x"), pos.getInt("y"), pos.getInt("z")));
            }
        }
        // Migrate the old single-starter-village save format. It remains visible
        // to the admin commands, but no new natural village is marked by it.
        if (state.districts.isEmpty() && nbt.getBoolean("generated") && nbt.contains("centerX")) {
            BlockPos old = new BlockPos(nbt.getInt("centerX"), nbt.getInt("centerY"), nbt.getInt("centerZ"));
            state.districts.put(key(old), old);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound stored = new NbtCompound();
        for (var entry : districts.entrySet()) {
            NbtCompound pos = new NbtCompound();
            pos.putInt("x", entry.getValue().getX());
            pos.putInt("y", entry.getValue().getY());
            pos.putInt("z", entry.getValue().getZ());
            stored.put(entry.getKey(), pos);
        }
        nbt.put("districts", stored);
        return nbt;
    }

    public static VillageState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) throw new IllegalStateException("Overworld not loaded");
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public static String key(BlockPos villageCenter) {
        // A 16-block grid makes minor villager movement irrelevant while keeping
        // two genuinely separate villages distinct.
        return Math.floorDiv(villageCenter.getX(), 16) + ":" + Math.floorDiv(villageCenter.getZ(), 16);
    }

    public boolean contains(BlockPos villageCenter) { return districts.containsKey(key(villageCenter)); }

    public void markGenerated(BlockPos villageCenter, BlockPos districtOrigin) {
        districts.put(key(villageCenter), districtOrigin.toImmutable());
        markDirty();
    }

    public List<Map.Entry<String, BlockPos>> allDistricts() {
        return List.copyOf(districts.entrySet());
    }

    public int districtCount() { return districts.size(); }

    // Compatibility helpers for existing admin/status commands.
    public boolean isGenerated() { return !districts.isEmpty(); }
    public BlockPos getCenter() { return districts.isEmpty() ? null : districts.values().iterator().next(); }

    public void setCenter(BlockPos pos) { markGenerated(pos, pos); }

    public void clear() {
        districts.clear();
        markDirty();
    }
}
