package com.siirio.jemcompat.mixin.worldtier;

import com.siirio.jemcompat.worldtier.BossMechanicPolicy;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss", remap = false)
public abstract class CaptainCorneliaHealingMixin {
    private static final float NATIVE_EMERGENCY_THRESHOLD = 20.0f;
    private static final float NATIVE_MAXIMUM_HEALTH = 300.0f;

    @ModifyConstant(method = "m_6075_", constant = @Constant(floatValue = NATIVE_EMERGENCY_THRESHOLD))
    private float jemcompat$scaleEmergencyHealingThreshold(float nativeThreshold) {
        LivingEntity boss = (LivingEntity) (Object) this;
        return BossMechanicPolicy.scaledAbsoluteThreshold(nativeThreshold, NATIVE_MAXIMUM_HEALTH, boss.getMaxHealth());
    }
}
