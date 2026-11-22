package com.siirio.jemserver.mixin.compat;

import com.siirio.jemserver.smp.events.BloodMoon;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.eternal_creations.archies_dino_mod.util.handlers.SculksaurHandler",remap=false)
public abstract class ArchiesSculksaurHandlerMixin {
    @Inject(method="trySummon",at=@At("HEAD"),cancellable=true)
    private void jem$excludeBloodMoonWarden(Warden warden,CallbackInfo callback) {
        if(warden.getPersistentData().hasUUID(BloodMoon.EVENT_ID)) callback.cancel();
    }
}
