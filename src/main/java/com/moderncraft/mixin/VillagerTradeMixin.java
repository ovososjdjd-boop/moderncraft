package com.moderncraft.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes vanilla villager offers. Moderncraft's economy is the only trading system. */
@Mixin(VillagerEntity.class)
public abstract class VillagerTradeMixin {
    @Inject(method = "setOffers", at = @At("HEAD"))
    private void moderncraft$clearVanillaOffers(TradeOfferList offers, CallbackInfo ci) {
        offers.clear();
    }
}
