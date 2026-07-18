package com.moderncraft.economy;

import com.moderncraft.Moderncraft;
import com.moderncraft.economy.bank.BankBlockEntity;
import com.moderncraft.economy.factory.FactoryBlockEntity;
import com.moderncraft.economy.factory.FactoryNoticeBoardBlockEntity;
import com.moderncraft.economy.jobs.JobBoardBlockEntity;
import com.moderncraft.economy.pickup.PickupPointBlockEntity;
import com.moderncraft.economy.stock.StockExchangeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Centralised block-entity registrations. Future buildings add their own
 * BlockEntityType here.
 */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final BlockEntityType<PickupPointBlockEntity> PICKUP_POINT_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "pickup_point"),
                    FabricBlockEntityTypeBuilder.create(
                            PickupPointBlockEntity::new,
                            ModBlocks.PICKUP_POINT
                    ).build()
            );

    public static final BlockEntityType<FactoryBlockEntity> FACTORY_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "factory"),
                    FabricBlockEntityTypeBuilder.create(
                            FactoryBlockEntity::new,
                            com.moderncraft.economy.factory.FactoryBlocks.BASE
                    ).build()
            );

    public static final BlockEntityType<FactoryNoticeBoardBlockEntity> FACTORY_NOTICE_BOARD_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "factory_notice_board"),
                    FabricBlockEntityTypeBuilder.create(
                            FactoryNoticeBoardBlockEntity::new,
                            com.moderncraft.economy.factory.FactoryBlocks.NOTICE_BOARD
                    ).build()
            );

    public static final BlockEntityType<BankBlockEntity> BANK_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "bank"),
                    FabricBlockEntityTypeBuilder.create(
                            BankBlockEntity::new,
                            com.moderncraft.economy.bank.BankBlocks.BANK
                    ).build()
            );

    public static final BlockEntityType<StockExchangeBlockEntity> STOCK_EXCHANGE_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "stock_exchange"),
                    FabricBlockEntityTypeBuilder.create(
                            StockExchangeBlockEntity::new,
                            com.moderncraft.economy.stock.StockBlocks.STOCK_EXCHANGE
                    ).build()
            );

    public static final BlockEntityType<JobBoardBlockEntity> CAFE_BOARD_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "cafe_board"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new JobBoardBlockEntity(CAFE_BOARD_BE, "cafe", pos, state),
                            com.moderncraft.economy.jobs.JobBlocks.CAFE_BOARD
                    ).build()
            );

    public static final BlockEntityType<JobBoardBlockEntity> LOADER_BOARD_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "loader_board"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new JobBoardBlockEntity(LOADER_BOARD_BE, "loader", pos, state),
                            com.moderncraft.economy.jobs.JobBlocks.LOADER_BOARD
                    ).build()
            );

    public static final BlockEntityType<LoaderTargetBlockEntity> LOADER_TARGET_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "loader_target"),
                    FabricBlockEntityTypeBuilder.create(
                            LoaderTargetBlockEntity::new,
                            com.moderncraft.economy.jobs.JobBlocks.LOADER_TARGET
                    ).build()
            );

    public static void register() {
        Moderncraft.LOGGER.info("[moderncraft] registered {} mod block-entity type(s)", 8);
    }
}
