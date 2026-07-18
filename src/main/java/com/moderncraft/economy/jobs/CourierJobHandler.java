package com.moderncraft.economy.jobs;

import com.moderncraft.economy.phone.PhoneNetworking;
import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.state.EconomyService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/** Handles the destination side of courier deliveries: a parcel is delivered to a villager. */
public final class CourierJobHandler {
    private CourierJobHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(entity instanceof VillagerEntity villager)) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            CafeCourierEntity courier = world.getEntitiesByClass(
                    CafeCourierEntity.class,
                    villager.getBoundingBox().expand(64.0),
                    c -> c.activeOrder() != null
                            && c.activeOrder().recipientId().equals(villager.getUuid())
                            && !c.activeOrder().expired(world.getTime())
            ).stream().findFirst().orElse(null);
            if (courier == null) {
                serverPlayer.sendMessage(Text.literal("Villager trading is replaced by Moderncraft jobs and the market."), true);
                return ActionResult.CONSUME;
            }

            CourierOrder order = courier.activeOrder();
            int have = count(player, order.itemId());
            if (have < order.count()) {
                serverPlayer.sendMessage(Text.literal("Courier parcel: " + order.count() + " × "
                        + displayName(order.itemId()) + ". You have " + have + "."), true);
                return ActionResult.CONSUME;
            }
            remove(player, order.itemId(), order.count());
            EconomyService.creditWallet(serverPlayer.getServer(), serverPlayer.getUuid(), order.reward());
            courier.completeOrder();
            PhoneNetworking.sendInfo(serverPlayer, "Parcel delivered to the villager. Earned " + order.reward() + " M$.");
            PhoneNetworking.syncBalances(serverPlayer, serverPlayer.getServer());
            return ActionResult.SUCCESS;
        });
    }

    private static int count(PlayerEntity player, String id) {
        var item = Registries.ITEM.get(Identifier.tryParse(id));
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private static void remove(PlayerEntity player, String id, int amount) {
        var item = Registries.ITEM.get(Identifier.tryParse(id));
        for (int i = 0; i < player.getInventory().size() && amount > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
            }
        }
    }

    private static String displayName(String id) {
        var item = Registries.ITEM.get(Identifier.tryParse(id));
        return item == null ? id : new ItemStack(item).getName().getString();
    }
}
