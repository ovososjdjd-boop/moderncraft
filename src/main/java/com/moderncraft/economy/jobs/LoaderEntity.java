package com.moderncraft.economy.jobs;

import com.moderncraft.Moderncraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The "Loader" — a heavy-block delivery NPC. Like the courier, but:
 * <ul>
 *     <li>The destination is a specific BlockPos in the world, not 'the NPC itself'.</li>
 *     <li>The items are 'heavy' (iron_block, gold_block, obsidian, anvil).</li>
 *     <li>The reward is 2x the courier's (heavier work pays more).</li>
 *     <li>While the player is carrying the load, particles trail behind them
 *         so the route is visible (cosmetic — not required to complete the job).</li>
 * </ul>
 */
public class LoaderEntity extends PathAwareEntity {

    private LoaderOrder activeOrder = null;
    private long lastParticleTick = 0L;
    private int ticksUntilNextOrder = 600; // 30s initial delay

    public LoaderEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22);
    }

    public LoaderOrder activeOrder() { return activeOrder; }

    /** Called by LoaderTarget when the player completes a delivery. */
    public void clearOrder() {
        this.activeOrder = null;
        this.setGlowing(false);
        // Re-queue a new order on a short delay.
        this.ticksUntilNextOrder = 90 * 20 + this.getWorld().getRandom().nextInt(30 * 20);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("TicksUntilNextOrder", ticksUntilNextOrder);
        if (activeOrder != null) {
            nbt.putString("OrderItem", activeOrder.itemId());
            nbt.putInt("OrderCount", activeOrder.count());
            nbt.putLong("OrderReward", activeOrder.reward());
            nbt.putInt("OrderX", activeOrder.destination().getX());
            nbt.putInt("OrderY", activeOrder.destination().getY());
            nbt.putInt("OrderZ", activeOrder.destination().getZ());
            nbt.putLong("OrderExpiresAt", activeOrder.expiresAt());
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ticksUntilNextOrder = nbt.getInt("TicksUntilNextOrder");
        if (nbt.contains("OrderItem") && nbt.contains("OrderX") && nbt.contains("OrderY") && nbt.contains("OrderZ")
                && nbt.contains("OrderExpiresAt")) {
            activeOrder = new LoaderOrder(nbt.getString("OrderItem"), nbt.getInt("OrderCount"), nbt.getLong("OrderReward"),
                    new BlockPos(nbt.getInt("OrderX"), nbt.getInt("OrderY"), nbt.getInt("OrderZ")),
                    nbt.getLong("OrderExpiresAt"));
            setGlowing(true);
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.3));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) return;
        ServerWorld sw = (ServerWorld) this.getWorld();
        long now = sw.getTime();

        if (activeOrder != null && activeOrder.expired(now)) {
            Moderncraft.LOGGER.info("Loader order expired: {}", activeOrder.itemId());
            clearOrder();
        }

        if (activeOrder == null) {
            ticksUntilNextOrder--;
            if (ticksUntilNextOrder <= 0) {
                activeOrder = generateOrder(sw);
                if (activeOrder != null) {
                    this.setGlowing(true);
                    Moderncraft.LOGGER.info("Loader issued order: deliver {} x{} to {} for {} M$",
                            activeOrder.itemId(), activeOrder.count(),
                            activeOrder.destination(), activeOrder.reward());
                }
                ticksUntilNextOrder = 90 * 20 + sw.random.nextInt(30 * 20);
            }
        } else {
            if (now - lastParticleTick >= 40) {
                lastParticleTick = now;
                spawnHighlightParticles(sw);
            }
        }
    }

    private void spawnHighlightParticles(ServerWorld sw) {
        // Slightly different particles than courier — campfire / smoke for a
        // "heavy work" feel.
        for (int i = 0; i < 4; i++) {
            double x = this.getX() + (sw.random.nextDouble() - 0.5) * 0.5;
            double y = this.getY() + 2.0 + i * 0.1;
            double z = this.getZ() + (sw.random.nextDouble() - 0.5) * 0.5;
            sw.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    private LoaderOrder generateOrder(ServerWorld sw) {
        // Heavy items: a small fixed list, all with meaningful sell prices.
        String[] heavy = {
                "minecraft:iron_block",
                "minecraft:gold_block",
                "minecraft:obsidian",
                "minecraft:anvil",
                "minecraft:diamond_block",
                "minecraft:chest",
                "minecraft:bookshelf",
                "minecraft:lectern",
                "minecraft:barrel"
        };
        String id = heavy[sw.random.nextInt(heavy.length)];
        int count = 1 + sw.random.nextInt(3);
        // Pick a destination within the village: any of the village buildings
        // the generator placed (we don't know their position here, so we
        // approximate by sampling around the loader's position).
        BlockPos origin = this.getBlockPos();
        BlockPos dest = pickTarget(sw, origin, 24);
        if (dest == null) return null;
        // Reward = a base fee + per-block price (sells are unreliable for heavy
        // items, so we just use a fixed table).
        long perItem = switch (id) {
            case "minecraft:iron_block" -> 360L;
            case "minecraft:gold_block" -> 720L;
            case "minecraft:obsidian" -> 60L;
            case "minecraft:anvil" -> 600L;
            case "minecraft:diamond_block" -> 3600L;
            case "minecraft:chest", "minecraft:lectern" -> 450L;
            case "minecraft:bookshelf", "minecraft:barrel" -> 220L;
            default -> 100L;
        };
        long reward = perItem * count + 50L;
        long expiresAt = sw.getTime() + 20L * 60L * 10L;
        return new LoaderOrder(id, count, reward, dest, expiresAt);
    }

    /** Selects one of the real yellow target blocks placed in the village. */
    private BlockPos pickTarget(ServerWorld world, BlockPos origin, int radius) {
        List<BlockPos> targets = new ArrayList<>();
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int y = origin.getY() - 4; y <= origin.getY() + 8; y++) {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).isOf(JobBlocks.LOADER_TARGET)) targets.add(pos.toImmutable());
                }
            }
        }
        return targets.isEmpty() ? null : targets.get(world.random.nextInt(targets.size()));
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.CONSUME;

        if (activeOrder == null) {
            sp.sendMessage(Text.literal("The loader has no open jobs. Check back in a minute or two."), true);
            return ActionResult.CONSUME;
        }
        // Tell the player what to do.
        BlockPos d = activeOrder.destination();
        sp.sendMessage(Text.literal("Deliver " + activeOrder.count() + " × " +
                displayNameOf(activeOrder.itemId()) + " to " +
                d.getX() + ", " + d.getY() + ", " + d.getZ() +
                ". Reward: " + activeOrder.reward() + " M$. Deadline: "
                + Math.max(0L, (activeOrder.expiresAt() - this.getWorld().getTime()) / 20L) + " seconds."), false);
        // We don't take the items from the player here — delivery is
        // completed by interacting with the destination block (see LoaderTarget
        // for the matching logic). This makes 'delivery' feel like a real
        // trip, not a click.
        return ActionResult.CONSUME;
    }

    private static String displayNameOf(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) id = Identifier.of("minecraft", itemId);
        Item item = Registries.ITEM.get(id);
        if (item == null) return itemId;
        return new ItemStack(item).getName().getString();
    }
}
