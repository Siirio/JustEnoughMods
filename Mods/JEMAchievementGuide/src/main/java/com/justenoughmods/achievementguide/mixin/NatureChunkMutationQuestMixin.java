package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureChunkOrigins;
import com.justenoughmods.achievementguide.integration.nature.NatureHarvestProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class NatureChunkMutationQuestMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void jem$invalidatePhysicalOrigin(BlockPos pos, BlockState state, boolean moving,
                                              CallbackInfoReturnable<BlockState> callback) {
        BlockState old = callback.getReturnValue();
        if (old != null && !old.equals(state)) {
            var chunk = (LevelChunk) (Object) this;
            NatureChunkOrigins.remove(chunk, pos, old.getBlock() != state.getBlock());
            if (chunk.getLevel() instanceof ServerLevel level) NatureHarvestProof.changed(level, pos);
        }
    }
}
