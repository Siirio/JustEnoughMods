package com.justenoughmods.achievementguide.mixin;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import com.justenoughmods.achievementguide.client.AdvancementScreenEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = BetterAdvancementsScreen.class, remap = false)
public abstract class BetterAdvancementsScreenMixin {
    @Inject(method = "m_6375_(DDI)Z", at = @At("HEAD"))
    private void jem$openGuide(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        AdvancementScreenEvents.directClick((BetterAdvancementsScreen) (Object) this, mouseX, mouseY, button);
    }
}
