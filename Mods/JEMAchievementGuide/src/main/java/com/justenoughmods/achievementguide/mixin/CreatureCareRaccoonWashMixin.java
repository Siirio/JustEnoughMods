package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareWash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.RaccoonAIWash", remap = false)
public abstract class CreatureCareRaccoonWashMixin {
    @Inject(method = "m_8037_", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { CreatureCareWash.begin(); }
    @Inject(method = "m_8037_", at = @At("RETURN"))
    private void jem$end(CallbackInfo callback) { CreatureCareWash.end(); }
}
