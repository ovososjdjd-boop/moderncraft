package com.moderncraft.economy.factory;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The factory's working state. Holds:
 * <ul>
 *     <li>{@code structureValid} — does the building have chimney, pipe and gear?</li>
 *     <li>{@code shiftOwner} — UUID of the player currently doing a shift (null if none).</li>
 *     <li>{@code shiftTicks} — how many ticks the current shift has been running.</li>
 *     <li>{@code produced} — list of item ids the factory has produced since the
 *         player started the shift. Cleared when claimed.</li>
 * </ul>
 * The factory runs at a fixed rate: one "production tick" per 100 game ticks (5
 * seconds) when a shift is active. Each tick it has a chance to produce one of
 * the configured outputs. The worker NPC pays the player when the shift ends.
 */
public class FactoryBlockEntity extends BlockEntity {

    public static final int SHIFT_DURATION_TICKS = 20 * 60 * 3; // 3 minutes
    public static final int TICK_INTERVAL = 100; // 5 seconds
    public static final float PRODUCTION_CHANCE = 0.65f;

    /** Production recipes. The weights make basic components common and complex parts rare. */
    public static final List<FactoryRecipe> RECIPES = List.of(
            new FactoryRecipe(Items.REDSTONE, 2, 5, 30),
            new FactoryRecipe(Items.REDSTONE_TORCH, 1, 3, 20),
            new FactoryRecipe(Items.REPEATER, 1, 2, 14),
            new FactoryRecipe(Items.COMPARATOR, 1, 2, 10),
            new FactoryRecipe(Items.PISTON, 1, 2, 8),
            new FactoryRecipe(Items.STICKY_PISTON, 1, 2, 6),
            new FactoryRecipe(Items.OBSERVER, 1, 1, 5),
            new FactoryRecipe(Items.HOPPER, 1, 1, 4)
    );

    private static int recipeWeightTotal() {
        return RECIPES.stream().mapToInt(FactoryRecipe::weight).sum();
    }

    private static FactoryRecipe chooseRecipe(java.util.Random random) {
        int roll = random.nextInt(recipeWeightTotal());
        for (FactoryRecipe recipe : RECIPES) {
            roll -= recipe.weight();
            if (roll < 0) return recipe;
        }
        return RECIPES.get(RECIPES.size() - 1);
    }

    private boolean structureValid = false;
    private UUID shiftOwner = null;
    private int shiftTicks = 0;
    private int ticksToNext = TICK_INTERVAL;
    private final List<ItemStack> produced = new ArrayList<>();

    public FactoryBlockEntity(BlockPos pos, BlockState state) {
        super(com.moderncraft.economy.ModBlockEntities.FACTORY_BE, pos, state);
    }

    public boolean isStructureComplete() { return structureValid; }

    public UUID shiftOwner() { return shiftOwner; }
    public int shiftTicks() { return shiftTicks; }
    public int shiftDuration() { return SHIFT_DURATION_TICKS; }
    public List<ItemStack> produced() { return produced; }

    public boolean hasOwner() { return shiftOwner != null; }
    public boolean isOwner(UUID playerId) { return playerId.equals(shiftOwner); }

    public void startShift(UUID playerId) {
        if (shiftOwner != null) return;
        this.shiftOwner = playerId;
        this.shiftTicks = 0;
        this.ticksToNext = TICK_INTERVAL;
        this.produced.clear();
        markDirty();
    }

    /** Called by the screen when the player ends the shift. Returns the items produced. */
    public List<ItemStack> endShift() {
        List<ItemStack> snapshot = new ArrayList<>(produced);
        this.shiftOwner = null;
        this.shiftTicks = 0;
        this.ticksToNext = TICK_INTERVAL;
        this.produced.clear();
        markDirty();
        return snapshot;
    }

    /** Server tick — called from {@code FactoryBlocks.ticker} each game tick. */
    public static void serverTick(World world, BlockPos pos, BlockState state, FactoryBlockEntity be) {
        if (world.isClient) return;

        if (be.shiftOwner == null) return;
        be.shiftTicks++;
        be.ticksToNext--;
        if (be.ticksToNext <= 0) {
            be.ticksToNext = TICK_INTERVAL;
            if (world.random.nextFloat() < PRODUCTION_CHANCE) {
                FactoryRecipe recipe = chooseRecipe(world.random);
                int amount = recipe.minCount() + world.random.nextInt(recipe.maxCount() - recipe.minCount() + 1);
                be.produced.add(new ItemStack(recipe.output(), amount));
            }
            // Cosmetic: gear rotates, smoke particles.
            BlockPos gearPos = pos.up();
            FactoryGearBlock.tickRotation(world, gearPos);
            if (world instanceof ServerWorld sw) {
                BlockPos chimneyPos = pos.up(2);
                if (sw.getBlockState(chimneyPos).getBlock() == FactoryBlocks.CHIMNEY) {
                    sw.spawnParticles(ParticleTypes.SMOKE,
                            chimneyPos.getX() + 0.5, chimneyPos.getY() + 1.0, chimneyPos.getZ() + 0.5,
                            3, 0.1, 0.05, 0.1, 0.01);
                }
            }
            be.markDirty();
        }

        // Push state to the owner every second (20 ticks) so the UI updates.
        if (be.shiftTicks % 20 == 0 && be.shiftOwner != null
                && world instanceof ServerWorld sw) {
            var owner = sw.getPlayerByUuid(be.shiftOwner);
            if (owner != null) {
                com.moderncraft.economy.factory.FactoryNetworking.sendState(owner, be);
            }
        }

        // Auto-end after the shift duration.
        if (be.shiftTicks >= SHIFT_DURATION_TICKS) {
            // Just stop the shift; the player needs to be online to claim.
            // If the owner logged out, we just abandon the shift silently.
            UUID owner = be.shiftOwner;
            if (world instanceof ServerWorld sw) {
                if (sw.getPlayerByUuid(owner) == null) {
                    be.shiftOwner = null;
                    be.shiftTicks = 0;
                    be.produced.clear();
                    be.markDirty();
                }
            }
        }
    }

    /** Validates the structure: chimney above, pipe to the side, gear immediately above. */
    public void markStructureDirty() {
        World w = getWorld();
        if (w == null) {
            this.structureValid = false;
            return;
        }
        BlockPos pos = getPos();
        BlockState up = w.getBlockState(pos.up());
        BlockState up2 = w.getBlockState(pos.up(2));
        boolean gearOk = up.getBlock() == FactoryBlocks.GEAR;
        boolean chimneyOk = up2.getBlock() == FactoryBlocks.CHIMNEY;
        boolean pipeOk = false;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (w.getBlockState(pos.offset(dir)).getBlock() == FactoryBlocks.PIPE) {
                pipeOk = true;
                break;
            }
        }
        this.structureValid = gearOk && chimneyOk && pipeOk;
    }

    // --- persistence --------------------------------------------------------

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putBoolean("structureValid", structureValid);
        nbt.putInt("shiftTicks", shiftTicks);
        nbt.putInt("ticksToNext", ticksToNext);
        if (shiftOwner != null) nbt.putUuid("shiftOwner", shiftOwner);
        NbtCompound items = new NbtCompound();
        for (int i = 0; i < produced.size(); i++) {
            ItemStack s = produced.get(i);
            items.putString("id" + i, Registries.ITEM.getId(s.getItem()).toString());
            items.putInt("count" + i, s.getCount());
        }
        items.putInt("size", produced.size());
        nbt.put("produced", items);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.structureValid = nbt.getBoolean("structureValid");
        this.shiftTicks = nbt.getInt("shiftTicks");
        this.ticksToNext = nbt.getInt("ticksToNext");
        if (nbt.contains("shiftOwner")) this.shiftOwner = nbt.getUuid("shiftOwner");
        produced.clear();
        if (nbt.contains("produced")) {
            NbtCompound items = nbt.getCompound("produced");
            int size = items.getInt("size");
            for (int i = 0; i < size; i++) {
                String idStr = items.getString("id" + i);
                int count = items.getInt("count" + i);
                var ident = net.minecraft.util.Identifier.tryParse(idStr);
                if (ident != null) {
                    Item item = Registries.ITEM.get(ident);
                    if (item != null) produced.add(new ItemStack(item, count));
                }
            }
        }
    }
}
