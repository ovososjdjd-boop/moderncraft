package com.moderncraft.client.jobs;

import com.moderncraft.economy.jobs.CafeCourierEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * A simple "head + body" silhouette renderer for the cafe courier. We
 * intentionally don't try to render a humanoid mesh here — the courier
 * is invisible except for its glowing outline (setGlowing(true) on the
 * server) and the happy-villager particles it spawns above its head. The
 * entity is essentially a glowing point of reference that walks around
 * the cafe plaza; players find it because it shines.
 * <p>
 * This keeps the v1 footprint small: no custom model, no texture, no
 * AI animation. If we want a proper villager-shape model later, we can
 * replace this renderer with one that uses vanilla's villager model.
 */
public class CafeCourierRenderer extends EntityRenderer<CafeCourierEntity> {

    public CafeCourierRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        // shadow radius: smaller than the default player shadow.
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(CafeCourierEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // We render nothing — the courier is "invisible" but the entity
        // hitbox is real (players can right-click it), and the server
        // already handles the glowing + particles.
        // A future renderer can add a textured humanoid here.
    }

    @Override
    public Identifier getTexture(CafeCourierEntity entity) {
        return Identifier.of("moderncraft", "textures/entity/cafe_courier.png");
    }
}
