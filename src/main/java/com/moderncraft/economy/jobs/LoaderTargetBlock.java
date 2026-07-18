package com.moderncraft.economy.jobs;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The "delivery point" for loader jobs. When a loader is given an order, the
 * destination is a real BlockPos of a LoaderTarget block. The player walks
 * to it, right-clicks, and if they're carrying the right item count, the
 * server credits the wallet.
 * <p>
 * For v1 we don't have a real persistent destination for a loader (loaders
 * spawn fresh on world load). The VillageGenerator places several
 * LoaderTarget blocks around the village as 'mailboxes'. The loader picks
 * one of them as its destination. Players can also place LoaderTarget
 * blocks manually for custom routes.
 */
public class LoaderTargetBlock extends BlockWithEntity {

    public LoaderTargetBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LoaderTargetBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.CONSUME;
        if (!(world.getBlockEntity(pos) instanceof LoaderTargetBlockEntity be)) return ActionResult.CONSUME;

        // Find a loader within 32 blocks that has an order targeting this position.
        LoaderEntity loader = findLoaderWithOrder(world, pos, 32);
        if (loader == null || loader.activeOrder() == null) {
            sp.sendMessage(Text.literal("There's no loader waiting for delivery here."), true);
            return ActionResult.CONSUME;
        }
        var order = loader.activeOrder();
        // Count the items in the player's inventory.
        int have = countInInventory(sp, order.itemId());
        if (have < order.count()) {
            sp.sendMessage(Text.literal("You need " + order.count() + " × " +
                    displayNameOf(order.itemId()) + " to complete this delivery. You have " + have + "."), true);
            return ActionResult.CONSUME;
        }
        // Take the items.
        int remaining = order.count();
        for (int i = 0; i < sp.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = sp.getInventory().getStack(i);
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id != null && id.toString().equals(order.itemId())) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            sp.sendMessage(Text.literal("Hmm, the items vanished. Try again."), true);
            return ActionResult.CONSUME;
        }
        // Pay and clear.
        long reward = order.reward();
        com.moderncraft.economy.state.EconomyService.creditWallet(
                sp.getServer(), sp.getUuid(), reward);
        com.moderncraft.economy.state.EconomyService.recordEvent(sp.getServer(), sp.getUuid(), "loader", reward,
                "Heavy load delivered");
        com.moderncraft.economy.phone.PhoneNetworking.sendInfo(sp,
                "Delivered " + order.count() + " × " + displayNameOf(order.itemId())
                        + ". Earned " + reward + " M$.");
        com.moderncraft.economy.phone.PhoneNetworking.syncBalances(sp, sp.getServer());
        loader.clearOrder();
        // Cosmetic: spawn particles at the delivery point.
        if (world instanceof ServerWorld sw) {
            for (int i = 0; i < 10; i++) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        1, 0.3, 0.2, 0.3, 0.0);
            }
        }
        return ActionResult.CONSUME;
    }

    private static LoaderEntity findLoaderWithOrder(World world, BlockPos target, int radius) {
        for (var entity : world.getEntitiesByClass(
                LoaderEntity.class,
                new net.minecraft.util.math.Box(target).expand(radius),
                e -> e.activeOrder() != null
                        && !e.activeOrder().expired(world.getTime())
                        && e.activeOrder().destination() != null
                        && e.activeOrder().destination().equals(target))) {
            return entity;
        }
        return null;
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
