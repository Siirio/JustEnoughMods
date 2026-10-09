package com.siirio.jemserver.mixin;

import com.siirio.jemserver.smp.events.BossSolidBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class BossSolidBoundaryMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 jem$bossSolidBoundary(Vec3 movement) {
        return BossSolidBoundary.collide((Entity) (Object) this, movement);
    }
}
