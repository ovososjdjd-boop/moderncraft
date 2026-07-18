package com.moderncraft.economy.phone;

import com.moderncraft.economy.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Right-click behaviour for the phone item. Sends a network request to the
 * server, which replies with the player's balances and triggers the client
 * to open the phone screen.
 */
public final class PhoneItems {

    private PhoneItems() {}

    public static void register() {
        UseItemCallback.EVENT.register(PhoneItems::onUse);
    }

    private static TypedActionResult<ItemStack> onUse(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() != ModItems.PHONE) return TypedActionResult.pass(stack);

        if (world.isClient()) {
            PhoneNetworking.sendOpenRequest();
        }
        return TypedActionResult.success(stack);
    }
}
