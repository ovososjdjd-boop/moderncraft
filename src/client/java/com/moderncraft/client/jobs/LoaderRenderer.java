package com.moderncraft.client.jobs;

import com.moderncraft.economy.jobs.LoaderEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Renders the loader with the vanilla player model until a dedicated worker model is added. */
public final class LoaderRenderer extends MobEntityRenderer<LoaderEntity, BipedEntityModel<LoaderEntity>> {
    public LoaderRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(LoaderEntity entity) {
        return Identifier.of("minecraft", "textures/entity/player/wide/alex.png");
    }
}
