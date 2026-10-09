package com.siirio.jemserver.mixin.staging;

import com.siirio.jemserver.smp.events.StructureStaging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfFire_Block_Entity", remap = false)
public abstract class FireAltarStagingMixin {
    @Inject(method = "commonTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$waitForStart(Level level, BlockPos position, BlockState state, @Coerce Object altar, CallbackInfo callback) {
        if (!StructureStaging.allowNativeSpawn(level, position)) callback.cancel();
    }
}
