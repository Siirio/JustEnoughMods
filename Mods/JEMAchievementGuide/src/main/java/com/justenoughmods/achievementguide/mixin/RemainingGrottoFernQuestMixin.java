package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.ai.GrottoceratopsEatPlantsGoal", remap = false)
public abstract class RemainingGrottoFernQuestMixin {
    @Unique private BlockPos jem$selected;
    @Unique private NativeFoodSources.Plant jem$fern;
    @Inject(method = "m_8037_", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$selected = null; jem$fern = null; }
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState jem$nativeSelected(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (level instanceof ServerLevel server && NativeFoodSources.block(state).equals("alexscaves:curly_fern")) {
            var planted = NativePlantProof.original(server, pos, state);
            if (planted != null && planted.kind().equals("curly_fern")) { jem$selected = pos.immutable(); jem$fern = planted; }
        }
        return state;
    }
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$nativeFiddlehead(Level level, BlockPos pos, BlockState result) {
        boolean accepted = level.setBlockAndUpdate(pos, result);
        if (accepted && level instanceof ServerLevel server && jem$fern != null && pos.equals(jem$selected)
                && NativeFoodSources.block(result).equals("alexscaves:fiddlehead") && server.getBlockState(pos).is(result.getBlock())) {
            NativeFoodSources.get(server).plant(server, pos, new NativeFoodSources.Plant(jem$fern.actor(), "fiddlehead_output", NativeFoodSources.block(result)));
        }
        return accepted;
    }
    @Inject(method = "m_8037_", at = @At("RETURN"))
    private void jem$end(CallbackInfo callback) { jem$selected = null; jem$fern = null; }
}
