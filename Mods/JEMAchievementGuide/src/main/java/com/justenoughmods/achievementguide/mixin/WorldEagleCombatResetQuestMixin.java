package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldEagleContributors;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatTracker.class)
public abstract class WorldEagleCombatResetQuestMixin {
    @Shadow @Final private LivingEntity mob;
    @Inject(method = "recheckStatus", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER))
    private void jem$nativeEncounterCleared(CallbackInfo ci) {
        if (mob instanceof WorldEagleContributors proof) proof.jem$eagleContributors().clear();
    }
}
