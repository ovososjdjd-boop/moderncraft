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

public final class LoaderEntities {

    private LoaderEntities() {}

    public static final EntityType<LoaderEntity> LOADER =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Moderncraft.MOD_ID, "loader"),
                    EntityType.Builder.create(LoaderEntity::new, SpawnGroup.MISC)
                            .dimensions(EntityDimensions.fixed(0.7f, 2.1f))
                            .build()
            );

    public static final Item LOADER_SPAWN_EGG =
            Registry.register(
                    Registries.ITEM,
                    Identifier.of(Moderncraft.MOD_ID, "loader_spawn_egg"),
                    new SpawnEggItem(LOADER, 0x444444, 0xCC8844,
                            new Item.Settings())
            );

    public static void register() {
        FabricDefaultAttributeRegistry.register(LOADER, LoaderEntity.createAttributes());
        Moderncraft.LOGGER.info("[moderncraft] registered loader entity + spawn egg");
    }
}
