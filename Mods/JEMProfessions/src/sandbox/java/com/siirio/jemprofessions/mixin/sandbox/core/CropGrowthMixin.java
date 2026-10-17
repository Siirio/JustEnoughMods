package com.siirio.jemprofessions.mixin.sandbox.core;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropGrowthMixin {
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void jem$greenhouse(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) { SandboxCoreEffects.greenhouseGrowth(level, pos, random); }
}
