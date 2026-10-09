package com.siirio.jemcompat.mixin.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EyeOfEnder.class)
public abstract class EyeOfEnderMixin {
    @Redirect(
            method = "signalTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int jemcompat$alwaysSurvive(RandomSource random, int bound, BlockPos target) {
        return 1;
    }
}
