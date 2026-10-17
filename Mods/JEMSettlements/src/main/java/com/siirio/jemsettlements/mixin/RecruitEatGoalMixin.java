package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.compat.recruits.RecruitNeedsService;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.RecruitEatGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecruitEatGoal.class, remap = false)
public abstract class RecruitEatGoalMixin {
    @Shadow public AbstractRecruitEntity recruit;
    /** RETURN preserves native updateHunger/updateMorale clocks, suppressing only its competing serving. */
    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true, remap = true)
    private void jem$sharedServingPolicy(CallbackInfoReturnable<Boolean> cir) {
        if (RecruitNeedsService.managed(recruit)) cir.setReturnValue(false);
    }
}
