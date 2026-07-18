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
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The "Cafe courier" — a simple NPC that hands out delivery orders and
 * pays the player when they bring the right item. We use {@link PathAwareEntity}
 * as the base (NOT VillagerEntity) so we don't have to deal with the villager
 * brain / profession system. The entity looks like an invisible-but-glowing
 * NPC for v1; the spawn egg places it and a custom render can replace this
 * later without touching the courier logic.
 * <p>
 * Order lifecycle:
 * <ol>
 *     <li>No order -> wait ~1 minute, then generate one.</li>
 *     <li>With an active order: emit happy-villager particles every 2s
 *         and glow (setGlowing(true)).</li>
 *     <li>Player right-clicks while holding the requested item -> take
 *         the items, pay 1.5x the sell price, clear the order.</li>
 *     <li>If the player right-clicks without the items -> a hint message.</li>
 * </ol>
 */
public class CafeCourierEntity extends PathAwareEntity {

    private CourierOrder activeOrder = null;
    private long lastParticleTick = 0L;
    private int ticksUntilNextOrder = 600; // 30s initial delay

    public CafeCourierEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    public CourierOrder activeOrder() { return activeOrder; }

    /** Completes an order after the player delivers it to a villager. */
    public void completeOrder() {
        activeOrder = null;
        this.setGlowing(false);
        ticksUntilNextOrder = 60 * 20 + this.getWorld().getRandom().nextInt(30 * 20);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("TicksUntilNextOrder", ticksUntilNextOrder);
        if (activeOrder != null) {
            nbt.putString("OrderItem", activeOrder.itemId());
            nbt.putInt("OrderCount", activeOrder.count());
            nbt.putLong("OrderReward", activeOrder.reward());
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ticksUntilNextOrder = nbt.getInt("TicksUntilNextOrder");
        if (nbt.contains("OrderItem")) {
            activeOrder = new CourierOrder(nbt.getString("OrderItem"), nbt.getInt("OrderCount"), nbt.getLong("OrderReward"));
            setGlowing(true);
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.4));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) return;
        ServerWorld sw = (ServerWorld) this.getWorld();
        long now = sw.getTime();

        if (activeOrder == null) {
            ticksUntilNextOrder--;
            if (ticksUntilNextOrder <= 0) {
                activeOrder = generateOrder(sw.random);
                if (activeOrder != null) {
                    this.setGlowing(true);
                    Moderncraft.LOGGER.info("Cafe courier issued order: {} x{} for {} M$",
                            activeOrder.itemId(), activeOrder.count(), activeOrder.reward());
                }
                // Try again in 60-90 seconds regardless.
                ticksUntilNextOrder = 60 * 20 + sw.random.nextInt(30 * 20);
            }
        } else {
            if (now - lastParticleTick >= 40) {
                lastParticleTick = now;
                spawnHighlightParticles(sw);
            }
        }
    }

    private void spawnHighlightParticles(ServerWorld sw) {
        for (int i = 0; i < 5; i++) {
            double x = this.getX() + (sw.random.nextDouble() - 0.5) * 0.5;
            double y = this.getY() + 1.9 + i * 0.1;
            double z = this.getZ() + (sw.random.nextDouble() - 0.5) * 0.5;
            sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z,
                    1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    private CourierOrder generateOrder(Random random) {
        var cats = com.moderncraft.economy.price.PriceCatalog.INSTANCE.categories();
        if (cats.isEmpty()) return null;
        List<com.moderncraft.economy.price.ItemPrice> candidates = new ArrayList<>();
        for (var cat : cats) {
            for (var p : com.moderncraft.economy.price.PriceCatalog.INSTANCE.byCategory(cat.id())) {
                if (p.sell() > 0 && !p.id().endsWith("_spawn_egg")) candidates.add(p);
            }
        }
        if (candidates.isEmpty()) return null;
        var pick = candidates.get(random.nextInt(candidates.size()));
        int count = 1 + random.nextInt(7);
        long reward = Math.max(1L, (long)(pick.sell() * count * 1.5));
        return new CourierOrder(pick.id(), count, reward);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.CONSUME;
        if (activeOrder == null) {
            sp.sendMessage(Text.literal("The courier has no open orders. Check back soon."), true);
        } else {
            sp.sendMessage(Text.literal("Parcel: " + activeOrder.count() + " × "
                    + displayNameOf(activeOrder.itemId()) + ". Deliver it to a villager within the village. Reward: "
                    + activeOrder.reward() + " M$."), false);
        }
        return ActionResult.CONSUME;
    }

    private static int countInInventory(PlayerEntity player, String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) id = Identifier.of("minecraft", itemId);
        Item target = Registries.ITEM.get(id);
        if (target == null) return 0;
        int n = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == target) n += stack.getCount();
        }
        return n;
    }

    private static String displayNameOf(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) id = Identifier.of("minecraft", itemId);
        Item item = Registries.ITEM.get(id);
        if (item == null) return itemId;
        return new ItemStack(item).getName().getString();
    }
}
