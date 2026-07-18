package com.moderncraft.client.jobs;

import com.moderncraft.economy.jobs.CafeCourierEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Renders the courier as a visible vanilla-style humanoid instead of a hitbox with particles. */
public final class CafeCourierRenderer extends MobEntityRenderer<CafeCourierEntity, BipedEntityModel<CafeCourierEntity>> {
    public CafeCourierRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.4f);
    }

    @Override
    public Identifier getTexture(CafeCourierEntity entity) {
        return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }
}
