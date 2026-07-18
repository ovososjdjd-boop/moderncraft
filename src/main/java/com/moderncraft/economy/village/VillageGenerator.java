package com.moderncraft.economy.village;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.ModBlocks;
import com.moderncraft.economy.bank.BankBlocks;
import com.moderncraft.economy.factory.FactoryBlocks;
import com.moderncraft.economy.jobs.JobBlocks;
import com.moderncraft.economy.stock.StockBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

/**
 * Builds a ready-made moderncraft village on first world join.
 * <p>
 * The village is a 23x23 plaza surrounded by 8 buildings. Each building
 * is a small vanilla-block structure with one functional block inside.
 * The player can immediately start using the buildings — no further
 * placement required.
 *
 * <pre>
 *      N
 *  +----+----+----+----+
 *  | H1 | LB | FB | H2 |     LB = Loader Depot
 *  +----+----+----+----+     FB = Factory
 *  | PB | ST | PZ | BK |     PZ = Pickup Point
 *  +----+----+----+----+     BK = Bank
 *  | H3 | CB | NH | H4 |     ST = Stock Exchange
 *  +----+----+----+----+     CB = Cafe (job board)
 *                         NH = Notice Hub
 *                         PB, H1-H4 = small park / house
 * </pre>
 */
public final class VillageGenerator {

    private VillageGenerator() {}

    public static void generate(ServerWorld world, BlockPos center) {
        removeResidentsAndJobs(world, center);
        clearArea(world, center, 16);

        // Plaza floor: smooth_stone, 23x23, centred on anchor.
        placeFloor(world, center, 11, Blocks.SMOOTH_STONE);
        placeEdge(world, center, 11, Blocks.COBBLESTONE);

        // 4 lanterns at the corners.
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                world.setBlockState(center.add(dx * 10, 1, dz * 10), Blocks.LANTERN);
            }
        }

        // The 8 buildings, placed in a 3x3 grid around the centre. The centre
        // cell stays empty (with a small decoration).
        // North: Loader, Factory, House
        // Mid:  Cafe,   Pickup,    Bank
        // South: House,  Notice,   Stock
        placeFactory(world, center.add(-7, 1, -7));
        placePickup(world, center.add(0, 1, -7));
        placeLoaderDepot(world, center.add(7, 1, -7));
        placeCafe(world, center.add(-7, 1, 0));
        placeNoticeHub(world, center.add(0, 1, 0));
        placeBank(world, center.add(7, 1, 0));
        placeHouse(world, center.add(-7, 1, 7));
        placeStockExchange(world, center.add(0, 1, 7));
        placeHouse(world, center.add(7, 1, 7));
        placeResidents(world, center);
    }

    private static void placeResidents(ServerWorld world, BlockPos center) {
        spawnResident(world, center.add(-7, 1, 7), "Resident Mira");
        spawnResident(world, center.add(7, 1, 7), "Resident Oleg");
        spawnResident(world, center.add(0, 1, 10), "Resident Ada");
    }

    private static void spawnResident(ServerWorld world, BlockPos pos, String name) {
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) return;
        villager.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        villager.setAiDisabled(true);
        villager.setCustomName(net.minecraft.text.Text.literal(name));
        villager.setCustomNameVisible(true);
        world.spawnEntity(villager);
    }

    private static void removeResidentsAndJobs(ServerWorld world, BlockPos center) {
        Box area = new Box(center).expand(16.0);
        world.getOtherEntities(null, area, entity -> !(entity instanceof PlayerEntity))
                .forEach(Entity::discard);
    }

    /** Wipe a 23x23x5 area (so the generator is idempotent for testing). */
    public static void clearArea(ServerWorld world, BlockPos center, int half) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = 0; dy < 6; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    Block b = world.getBlockState(m).getBlock();
                    if (b != Blocks.BEDROCK) world.setBlockState(m, Blocks.AIR);
                }
            }
        }
    }

    // --- ground helpers -----------------------------------------------------

    private static void placeFloor(ServerWorld world, BlockPos center, int half, Block block) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                m.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                world.setBlockState(m, block.getDefaultState());
            }
        }
    }

    private static void placeEdge(ServerWorld world, BlockPos center, int half, Block block) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            m.set(center.getX() + dx, center.getY(), center.getZ() - half - 1);
            world.setBlockState(m, block.getDefaultState());
            m.set(center.getX() + dx, center.getY(), center.getZ() + half + 1);
            world.setBlockState(m, block.getDefaultState());
        }
        for (int dz = -half - 1; dz <= half + 1; dz++) {
            m.set(center.getX() - half - 1, center.getY(), center.getZ() + dz);
            world.setBlockState(m, block.getDefaultState());
            m.set(center.getX() + half + 1, center.getY(), center.getZ() + dz);
            world.setBlockState(m, block.getDefaultState());
        }
    }

    // --- building pieces (very simple) -------------------------------------

    private static void placeFactory(ServerWorld world, BlockPos origin) {
        // 3x3 iron_block floor.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.IRON_BLOCK.getDefaultState());
            }
        }
        world.setBlockState(origin, FactoryBlocks.BASE.getDefaultState());
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                for (int dy = 1; dy <= 3; dy++) {
                    world.setBlockState(origin.add(dx, dy, dz), Blocks.BRICK_SLAB.getDefaultState());
                }
            }
        }
        for (int dy = 4; dy <= 6; dy++) {
            world.setBlockState(origin.add(-1, dy, -1), FactoryBlocks.CHIMNEY.getDefaultState());
        }
        for (int dx = 1; dx <= 2; dx++) {
            world.setBlockState(origin.add(dx, 0, 0), FactoryBlocks.PIPE.getDefaultState());
        }
        world.setBlockState(origin.add(0, 1, 0), FactoryBlocks.GEAR.getDefaultState());
    }

    private static void placePickup(ServerWorld world, BlockPos origin) {
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.OAK_PLANKS.getDefaultState());
            }
        }
        world.setBlockState(origin, ModBlocks.PICKUP_POINT.getDefaultState());
        world.setBlockState(origin.add(0, 0, 1), Blocks.OAK_SLAB.getDefaultState());
        world.setBlockState(origin.add(0, 1, -1), Blocks.OAK_STAIRS.getDefaultState()
                .with(net.minecraft.block.StairBlock.FACING, net.minecraft.util.math.Direction.NORTH));
    }

    private static void placeNoticeHub(ServerWorld world, BlockPos origin) {
        world.setBlockState(origin.add(0, -1, 0), Blocks.OAK_PLANKS.getDefaultState());
        world.setBlockState(origin.add(0, 0, -1), Blocks.OAK_FENCE.getDefaultState());
        world.setBlockState(origin.add(0, 1, -1), Blocks.OAK_FENCE.getDefaultState());
        world.setBlockState(origin.add(0, 2, -1), Blocks.OAK_SIGN.getDefaultState());
        if (world.getBlockEntity(origin.add(0, 2, -1)) instanceof net.minecraft.block.entity.SignBlockEntity sign) {
            net.minecraft.block.entity.SignText text = sign.getFrontText()
                    .withMessage(0, net.minecraft.text.Text.literal("Moderncraft"))
                    .withMessage(1, net.minecraft.text.Text.literal("Village Centre"))
                    .withMessage(2, net.minecraft.text.Text.literal("- work, trade,"))
                    .withMessage(3, net.minecraft.text.Text.literal("and prosper"));
            sign.setText(text, true);
        }
    }

    private static void placeHouse(ServerWorld world, BlockPos origin) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.OAK_PLANKS.getDefaultState());
            }
        }
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0) {
                    world.setBlockState(origin.add(dx, dy, -1), Blocks.OAK_PLANKS.getDefaultState());
                    world.setBlockState(origin.add(dx, dy, 1), Blocks.OAK_PLANKS.getDefaultState());
                }
            }
            for (int dz = -1; dz <= 1; dz++) {
                if (dz != 0) {
                    world.setBlockState(origin.add(-1, dy, dz), Blocks.OAK_PLANKS.getDefaultState());
                    world.setBlockState(origin.add(1, dy, dz), Blocks.OAK_PLANKS.getDefaultState());
                }
            }
        }
        world.setBlockState(origin.add(0, 0, 1), Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, net.minecraft.util.math.Direction.SOUTH)
                .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
        world.setBlockState(origin.add(0, 1, 1), Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, net.minecraft.util.math.Direction.SOUTH)
                .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, 2, dz), Blocks.OAK_SLAB.getDefaultState());
            }
        }
        world.setBlockState(origin.add(0, 1, -1), Blocks.GLASS_PANE.getDefaultState());
    }

    private static void placeBank(ServerWorld world, BlockPos origin) {
        // 3x3 quartz_block floor (banks look 'clean').
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.QUARTZ_BLOCK.getDefaultState());
            }
        }
        // Walls: quartz_block on all 4 sides, 2 tall.
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0) {
                    world.setBlockState(origin.add(dx, dy, -1), Blocks.QUARTZ_BLOCK.getDefaultState());
                    world.setBlockState(origin.add(dx, dy, 1), Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
            for (int dz = -1; dz <= 1; dz++) {
                if (dz != 0) {
                    world.setBlockState(origin.add(-1, dy, dz), Blocks.QUARTZ_BLOCK.getDefaultState());
                    world.setBlockState(origin.add(1, dy, dz), Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }
        // The bank block in the middle, accessible from south (door gap).
        world.setBlockState(origin, BankBlocks.BANK.getDefaultState());
        // Counter: quartz_slab on the north side.
        world.setBlockState(origin.add(0, 0, -1), Blocks.QUARTZ_SLAB.getDefaultState());
        // Roof: quartz_slab on top.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, 2, dz), Blocks.QUARTZ_SLAB.getDefaultState());
            }
        }
    }

    private static void placeStockExchange(ServerWorld world, BlockPos origin) {
        // 3x3 dark_oak_planks floor.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.DARK_OAK_PLANKS.getDefaultState());
            }
        }
        // Low walls (1 block tall) of dark_oak_fence so the building feels open.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) == 2) {
                    world.setBlockState(origin.add(dx, 0, dz), Blocks.DARK_OAK_FENCE.getDefaultState());
                }
            }
        }
        world.setBlockState(origin, StockBlocks.STOCK_EXCHANGE.getDefaultState());
        // "Marquee" sign: dark_oak_sign on the north side.
        world.setBlockState(origin.add(0, 0, -1), Blocks.DARK_OAK_SIGN.getDefaultState());
        if (world.getBlockEntity(origin.add(0, 0, -1)) instanceof net.minecraft.block.entity.SignBlockEntity sign) {
            net.minecraft.block.entity.SignText text = sign.getFrontText()
                    .withMessage(0, net.minecraft.text.Text.literal("STOCK"))
                    .withMessage(1, net.minecraft.text.Text.literal("EXCHANGE"))
                    .withMessage(2, net.minecraft.text.Text.literal("buy low,"))
                    .withMessage(3, net.minecraft.text.Text.literal("sell high"));
            sign.setText(text, true);
        }
    }

    private static void placeCafe(ServerWorld world, BlockPos origin) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.SPRUCE_PLANKS.getDefaultState());
            }
        }
        world.setBlockState(origin, JobBlocks.CAFE_BOARD.getDefaultState());
        // Counter: spruce_slab.
        world.setBlockState(origin.add(0, 0, -1), Blocks.SPRUCE_SLAB.getDefaultState());
        // Awning: spruce_stairs on the north side, top of the building.
        world.setBlockState(origin.add(0, 1, -1), Blocks.SPRUCE_STAIRS.getDefaultState()
                .with(net.minecraft.block.StairBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        // Spawn the cafe courier NPC just in front of the counter.
        com.moderncraft.economy.jobs.CafeCourierEntity courier =
                com.moderncraft.economy.jobs.CourierEntities.CAFE_COURIER.create(world);
        courier.refreshPositionAndAngles(
                origin.getX() + 0.5, origin.getY(), origin.getZ() + 1.5,
                180f, 0f);
        courier.setCustomName(net.minecraft.text.Text.literal("Cafe Courier"));
        courier.setCustomNameVisible(true);
        world.spawnEntity(courier);
    }

    private static void placeLoaderDepot(ServerWorld world, BlockPos origin) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.STONE_BRICKS.getDefaultState());
            }
        }
        world.setBlockState(origin, JobBlocks.LOADER_BOARD.getDefaultState());
        // "Crane": iron bars above the board.
        for (int dy = 1; dy <= 2; dy++) {
            world.setBlockState(origin.add(0, dy, 0), Blocks.IRON_BARS.getDefaultState());
        }
        // A small chest to the side (decoration; not our chest, vanilla).
        world.setBlockState(origin.add(1, 0, 0), Blocks.CHEST.getDefaultState());
        // Spawn the loader NPC next to the depot.
        com.moderncraft.economy.jobs.LoaderEntity loader =
                com.moderncraft.economy.jobs.LoaderEntities.LOADER.create(world);
        loader.refreshPositionAndAngles(
                origin.getX() + 1.5, origin.getY(), origin.getZ() + 0.5,
                90f, 0f);
        loader.setCustomName(net.minecraft.text.Text.literal("Loader"));
        loader.setCustomNameVisible(true);
        world.spawnEntity(loader);

        // Place a LoaderTarget block in each corner of the plaza — these are
        // the destinations loaders can choose between. We use yellow wool so
        // they're easy to spot.
        placeLoaderTarget(world, origin.add(-12, 0, -12));
        placeLoaderTarget(world, origin.add(12, 0, -12));
        placeLoaderTarget(world, origin.add(-12, 0, 12));
        placeLoaderTarget(world, origin.add(12, 0, 12));
    }

    private static void placeLoaderTarget(ServerWorld world, BlockPos origin) {
        // Small 1x1 platform with a yellow wool marker and the target on top.
        world.setBlockState(origin, Blocks.STONE_BRICKS.getDefaultState());
        world.setBlockState(origin.add(0, 1, 0), Blocks.YELLOW_WOOL.getDefaultState());
        world.setBlockState(origin.add(0, 2, 0), JobBlocks.LOADER_TARGET.getDefaultState());
    }
}
