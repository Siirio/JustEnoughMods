package com.siirio.jemserver.mixin.events;

import com.siirio.jemserver.smp.events.BloodMoon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class BloodMoonWardenTargetMixin {
    @Inject(method="canTargetEntity",at=@At("HEAD"),cancellable=true)
    private void jem$ignoreBloodMoonSpectators(Entity target,CallbackInfoReturnable<Boolean> callback) {
        if(!BloodMoon.canTarget((Warden)(Object)this,target)) callback.setReturnValue(false);
    }
}
