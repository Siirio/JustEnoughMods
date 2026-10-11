package com.siirio.jemserver.mixin.teleport;

import com.siirio.jemserver.smp.events.SolidBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MobBoundaryTeleportMixin {
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void jem$boundaryDestination(double x, double y, double z, CallbackInfo callback) {
        if ((Entity) (Object) this instanceof Mob mob && !SolidBoundary.permitsTeleport(mob, new Vec3(x, y, z))) callback.cancel();
    }
}
