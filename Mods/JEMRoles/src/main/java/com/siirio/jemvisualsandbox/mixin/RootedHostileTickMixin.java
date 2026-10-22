package com.siirio.jemvisualsandbox.mixin;

import com.siirio.jemvisualsandbox.workbench.SandboxHostileControl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pause the whole hostile tick, including subclass fuse/spell/projectile logic. */
@Mixin(ServerLevel.class)
public abstract class RootedHostileTickMixin {
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void sandbox$rootedHostile(Entity entity, CallbackInfo callback) {
        if(SandboxHostileControl.pause(entity)) callback.cancel();
    }
    @Inject(method="tickNonPassenger",at=@At("TAIL"))
    private void sandbox$pinHostile(Entity entity, CallbackInfo callback) {
        SandboxHostileControl.pin(entity);
    }
}
