package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateControlProofs;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.KineticBlockEntity", remap = false)
public abstract class CreateControlInputQuestMixin {
    @Inject(method = "onSpeedChanged", at = @At("RETURN"), remap = false)
    private void jem$inputActuallyStopped(float oldSpeed, CallbackInfo callback) {
        CreateControlProofs.speedChanged((KineticBlockEntity)(Object)this, oldSpeed);
    }
}
