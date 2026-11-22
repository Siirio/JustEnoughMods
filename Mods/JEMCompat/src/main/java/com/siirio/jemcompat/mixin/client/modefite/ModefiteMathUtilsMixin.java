package com.siirio.jemcompat.mixin.client.modefite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "timmychips.modefiteitemdefinitions.comp.util.MathUtils", remap = false)
public abstract class ModefiteMathUtilsMixin {
    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemcompat$getPos(GlobalPos globalPos, CallbackInfoReturnable<BlockPos> result) {
        result.setReturnValue(globalPos == null ? null : globalPos.pos());
    }

    @Inject(method = "getDimension", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemcompat$getDimension(GlobalPos globalPos,
                                                CallbackInfoReturnable<ResourceKey<Level>> result) {
        result.setReturnValue(globalPos == null ? null : globalPos.dimension());
    }
}
