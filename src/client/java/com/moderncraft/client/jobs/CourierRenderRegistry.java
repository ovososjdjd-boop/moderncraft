package com.moderncraft.client.jobs;

import com.moderncraft.economy.jobs.CourierEntities;
import com.moderncraft.economy.jobs.LoaderEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Wires the courier and loader renderers. Called from ModerncraftClient. */
public final class CourierRenderRegistry {

    private CourierRenderRegistry() {}

    public static void register() {
        EntityRendererRegistry.register(CourierEntities.CAFE_COURIER, CafeCourierRenderer::new);
        EntityRendererRegistry.register(LoaderEntities.LOADER, LoaderRenderer::new);
    }
}
