package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityStuckSpeedAccessor {
    @Accessor("stuckSpeedMultiplier") void jemcompat$setStuckSpeedMultiplier(Vec3 multiplier);
}
