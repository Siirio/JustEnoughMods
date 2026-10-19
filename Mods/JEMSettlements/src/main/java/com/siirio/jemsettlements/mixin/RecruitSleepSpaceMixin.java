package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.happiness.BedPersonalSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recruits inherit this method; optional-mod membership belongs to the isolated service adapter. */
@Mixin(LivingEntity.class)
public abstract class RecruitSleepSpaceMixin {
    @Inject(method = "startSleeping(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void jem$personalBedSpace(BlockPos bed, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        // Villager's override must be cancelled before its own successful-sleep bookkeeping.
        if (!(entity instanceof Villager) && BedPersonalSpace.refuse(entity, bed)) ci.cancel();
    }
}
