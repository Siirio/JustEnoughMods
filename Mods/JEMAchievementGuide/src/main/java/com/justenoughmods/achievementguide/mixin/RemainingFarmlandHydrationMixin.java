package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeSoilProof;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class RemainingFarmlandHydrationMixin {
    @Unique private static final ThreadLocal<UUID> jem$sprinklerActor = new ThreadLocal<>();
    @Invoker("isNearWater") private static boolean jem$nativeWater(LevelReader level, BlockPos pos) { throw new AssertionError(); }
    @Inject(method = "randomTick", at = @At("HEAD"))
    private void jem$start(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) { jem$sprinklerActor.remove(); }
    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmBlock;isNearWater(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean jem$originalPredicate(LevelReader reader, BlockPos pos) {
        NativeSoilProof.begin(reader, pos);
        boolean result;
        try { result = jem$nativeWater(reader, pos); }
        catch (RuntimeException | Error failure) { NativeSoilProof.end(false); throw failure; }
        UUID placer = NativeSoilProof.end(result);
        if (placer != null) jem$sprinklerActor.set(placer);
        return result;
    }
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void jem$completed(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        UUID actor = jem$sprinklerActor.get(); jem$sprinklerActor.remove();
        NativeSoilProof.hydrated(level, pos, state, actor);
    }
}
