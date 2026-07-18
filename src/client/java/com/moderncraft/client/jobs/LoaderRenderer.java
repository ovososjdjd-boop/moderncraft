package com.moderncraft.client.jobs;

import com.moderncraft.economy.jobs.LoaderEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** No-draw renderer for the loader. Same approach as CafeCourierRenderer. */
public class LoaderRenderer extends EntityRenderer<LoaderEntity> {

    public LoaderRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(LoaderEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // intentionally empty
    }

    @Override
    public Identifier getTexture(LoaderEntity entity) {
        return Identifier.of("moderncraft", "textures/entity/loader.png");
    }
}
