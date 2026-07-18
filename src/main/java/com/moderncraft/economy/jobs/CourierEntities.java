package com.moderncraft.economy.jobs;

import com.moderncraft.Moderncraft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers the cafe courier entity type and its spawn egg.
 */
public final class CourierEntities {

    private CourierEntities() {}

    public static final EntityType<CafeCourierEntity> CAFE_COURIER =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "cafe_courier"),
                    EntityType.Builder.create(CafeCourierEntity::new, SpawnGroup.MISC)
                            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                            .build()
            );

    public static final Item CAFE_COURIER_SPAWN_EGG =
            Registry.register(
                    Registries.ITEM,
                    Identifier.of(Moderncraft.MOD_ID, "cafe_courier_spawn_egg"),
                    new SpawnEggItem(CAFE_COURIER, 0x8B4513, 0xFFD700,
                            new Item.Settings())
            );

    public static void register() {
        FabricDefaultAttributeRegistry.register(CAFE_COURIER, CafeCourierEntity.createAttributes());
        Moderncraft.LOGGER.info("[moderncraft] registered cafe_courier entity + spawn egg");
    }
}
