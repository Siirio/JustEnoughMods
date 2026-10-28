package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFertilizerProof;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTreeGrower.class)
public abstract class RemainingNativeTreeGrowthMixin {
    @Unique private final ThreadLocal<NativeFoodSources.Plant> jem$planted = new ThreadLocal<>();
    @Inject(method = "growTree", at = @At("HEAD"))
    private void jem$begin(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random, CallbackInfoReturnable<Boolean> callback) {
        jem$planted.remove(); var planted = NativePlantProof.original(level, pos, state);
        if (planted != null) jem$planted.set(planted);
    }
    @Inject(method = "growTree", at = @At("RETURN"))
    private void jem$grown(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random, CallbackInfoReturnable<Boolean> callback) {
        var planted = jem$planted.get(); jem$planted.remove();
        NativePlantProof.tree(level, pos, planted, state, callback.getReturnValue());
        if (callback.getReturnValue() && NativeFoodSources.block(state).equals("alexscaves:thornwood_sapling")
                && NativeFoodSources.block(level.getBlockState(pos)).equals("alexscaves:thornwood_log")) NativeFertilizerProof.grew(level);
    }
}
