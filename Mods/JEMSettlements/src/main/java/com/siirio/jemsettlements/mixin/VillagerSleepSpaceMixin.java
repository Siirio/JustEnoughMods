package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.happiness.BedPersonalSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refuse before Villager credits LAST_SLEPT and clears its walking memories. */
@Mixin(Villager.class)
public abstract class VillagerSleepSpaceMixin {
    @Inject(method = "startSleeping(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void jem$personalBedSpace(BlockPos bed, CallbackInfo ci) {
        if (BedPersonalSpace.refuse((Villager) (Object) this, bed)) ci.cancel();
    }
}
