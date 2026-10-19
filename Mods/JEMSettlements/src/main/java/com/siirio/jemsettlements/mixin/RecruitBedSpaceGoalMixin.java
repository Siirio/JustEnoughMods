package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.happiness.BedPersonalSpace;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.RestGoal;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block the native rest attempt before it separately writes the synchronized sleeping position. */
@Mixin(value = RestGoal.class, remap = false)
public abstract class RecruitBedSpaceGoalMixin {
    @Shadow @Final private AbstractRecruitEntity recruit;
    @Shadow private BlockPos sleepPos;

    @Inject(method = "goToBed(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void jem$personalBedSpace(BlockPos bed, CallbackInfo ci) {
        if (bed != null && BedPersonalSpace.refuse(recruit, bed)) { sleepPos = null; ci.cancel(); }
    }
}
