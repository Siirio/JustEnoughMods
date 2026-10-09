package com.siirio.jemcompat.mixin.worldtier;

import com.siirio.jemcompat.worldtier.BossMechanicPolicy;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "net.unusual.block_factorys_bosses.entity.boss.kraken.KrakenEntity", remap = false)
public abstract class KrakenStaggerMixin {
    private static final float NATIVE_STAGGER_THRESHOLD = 100.0f;
    private static final float NATIVE_MAXIMUM_HEALTH = 500.0f;

    @ModifyConstant(method = "m_6469_", constant = @Constant(floatValue = NATIVE_STAGGER_THRESHOLD))
    private float jemcompat$scaleStaggerThreshold(float nativeThreshold) {
        LivingEntity boss = (LivingEntity) (Object) this;
        return BossMechanicPolicy.scaledAbsoluteThreshold(nativeThreshold, NATIVE_MAXIMUM_HEALTH, boss.getMaxHealth());
    }
}
