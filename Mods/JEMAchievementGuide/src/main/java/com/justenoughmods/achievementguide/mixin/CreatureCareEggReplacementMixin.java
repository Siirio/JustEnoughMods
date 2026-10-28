package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class CreatureCareEggReplacementMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void jem$changed(BlockPos pos, BlockState after, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        if (callback.getReturnValue() != null && ((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level) {
            CreatureCareEggs.changed(level, pos, callback.getReturnValue(), after);
        }
    }
}
