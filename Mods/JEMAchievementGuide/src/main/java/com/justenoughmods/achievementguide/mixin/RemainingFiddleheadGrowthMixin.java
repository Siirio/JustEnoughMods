package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.block.FiddleheadBlock", remap = false)
public abstract class RemainingFiddleheadGrowthMixin {
    @Unique private final ThreadLocal<NativeFoodSources.Plant> jem$planted = new ThreadLocal<>();
    @Inject(method = "m_213898_", at = @At("HEAD"))
    private void jem$begin(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        jem$planted.remove(); var planted = NativePlantProof.original(level, pos, state);
        if (planted != null) jem$planted.set(planted);
    }
    @Inject(method = "m_213898_", at = @At("RETURN"))
    private void jem$grown(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        var planted = jem$planted.get(); jem$planted.remove();
        if (planted != null && NativeFoodSources.block(level.getBlockState(pos)).equals("alexscaves:curly_fern")) NativePlantProof.evolved(level, pos, planted, level.getBlockState(pos));
    }
}
