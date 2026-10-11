package com.siirio.jemserver.mixin;

import com.siirio.jemserver.smp.events.SolidBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SolidBoundaryMixin {
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void jem$solidBoundary(Vec3 movement, CallbackInfoReturnable<Vec3> callback) {
        callback.setReturnValue(SolidBoundary.collide((Entity) (Object) this, callback.getReturnValue()));
    }
}
