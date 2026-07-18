package com.moderncraft.economy.jobs;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A "job board" — a stand-alone notice board that opens a small message
 * about a job. We use one block for both the Cafe (couriers) and the Loader
 * Depot, distinguished by a 'role' field set in the BlockEntity. This is
 * the cheapest way to get two NPCs in the world without two distinct block
 * types.
 */
public class JobBoardBlock extends BlockWithEntity {

    private final String role;

    public JobBoardBlock(String role, AbstractBlock.Settings settings) {
        super(settings);
        this.role = role;
    }

    public String role() { return role; }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (this == com.moderncraft.economy.jobs.JobBlocks.CAFE_BOARD) {
            return new JobBoardBlockEntity(
                    com.moderncraft.economy.ModBlockEntities.CAFE_BOARD_BE, "cafe", pos, state);
        }
        if (this == com.moderncraft.economy.jobs.JobBlocks.LOADER_BOARD) {
            return new JobBoardBlockEntity(
                    com.moderncraft.economy.ModBlockEntities.LOADER_BOARD_BE, "loader", pos, state);
        }
        return new JobBoardBlockEntity(
                com.moderncraft.economy.ModBlockEntities.CAFE_BOARD_BE, role, pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        String message = switch (role) {
            case "cafe" -> "Cafe — couriers pick up delivery orders here. " +
                    "The courier job is coming in a future update.";
            case "loader" -> "Loader Depot — loaders carry heavy blocks and furniture " +
                    "to villagers. The loader job is coming in a future update.";
            default -> "Job board.";
        };
        player.sendMessage(Text.literal(message), true);
        return ActionResult.CONSUME;
    }
}
