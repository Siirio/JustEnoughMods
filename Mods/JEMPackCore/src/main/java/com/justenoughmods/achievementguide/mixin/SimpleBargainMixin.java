package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.api.bargain.BargainResult;
import tictim.paraglider.contents.recipe.SimpleBargain;

@Mixin(value = SimpleBargain.class, remap = false)
public abstract class SimpleBargainMixin {
    private static final ResourceLocation HEALTH = new ResourceLocation("jem_guide", "qol/13_3_1_1_1");
    private static final ResourceLocation STAMINA = new ResourceLocation("jem_guide", "qol/13_3_1_1_2");

    @Shadow
    @Final
    private int heartContainerOffers;

    @Shadow
    @Final
    private int staminaVesselOffers;

    @Inject(method = "bargain", at = @At("RETURN"))
    private void afterBargain(Player player, boolean simulate, CallbackInfoReturnable<BargainResult> cir) {
        if (simulate || !(player instanceof ServerPlayer serverPlayer) || !cir.getReturnValue().isSuccess()) {
            return;
        }
        if (heartContainerOffers > 0) {
            JemCriteria.fire(serverPlayer, HEALTH);
        }
        if (staminaVesselOffers > 0) {
            JemCriteria.fire(serverPlayer, STAMINA);
        }
    }
}
