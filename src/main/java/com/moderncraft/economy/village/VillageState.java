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

/**
 * Persistent, per-world state for the moderncraft village. Stores the
 * village center position and a 'generated' flag so we know whether the
 * world already has a village (we never want to spawn two).
 * <p>
 * We deliberately keep this small — the village is supposed to be one
 * structure per world, not a database.
 */
public final class VillageState extends PersistentState {

    private static final Identifier ID = Identifier.of(Moderncraft.MOD_ID, "village");

    private static final PersistentStateType<VillageState> TYPE = new PersistentStateType<>(
            ID,
            VillageState::new,
            VillageState::fromNbt,
            null
    );

    private BlockPos center = null;
    private boolean generated = false;

    public VillageState() {}

    public static VillageState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        VillageState s = new VillageState();
        if (nbt.contains("centerX")) {
            s.center = new BlockPos(
                    nbt.getInt("centerX"),
                    nbt.getInt("centerY"),
                    nbt.getInt("centerZ"));
        }
        s.generated = nbt.getBoolean("generated");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("generated", generated);
        if (center != null) {
            nbt.putInt("centerX", center.getX());
            nbt.putInt("centerY", center.getY());
            nbt.putInt("centerZ", center.getZ());
        }
        return nbt;
    }

    public static VillageState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException("Overworld not loaded");
        }
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public boolean isGenerated() { return generated; }
    public BlockPos getCenter() { return center; }

    public void setCenter(BlockPos pos) {
        this.center = pos;
        this.generated = true;
        markDirty();
    }

    /** Reset to "no village". Used by the admin /village reset command. */
    public void clear() {
        this.generated = false;
        this.center = null;
        markDirty();
    }
}
