package com.siirio.jemserver.mixin.client;

import com.siirio.jemserver.client.smp.EventAttackRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class EventSurfaceLevelMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void jemserver$invalidateChangedSurface(BlockPos position, BlockState state, int flags, int recursion,
                                                    CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) EventAttackRenderer.invalidateSurface(position);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void jemserver$invalidateUnloadedSurface(LevelChunk chunk, CallbackInfo callback) {
        EventAttackRenderer.invalidateSurface(chunk.getPos());
    }

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void jemserver$invalidateLoadedSurface(ChunkPos position, CallbackInfo callback) {
        EventAttackRenderer.invalidateSurface(position);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("RETURN"))
    private void jemserver$invalidateVerifiedSurface(BlockPos position, BlockState state, int flags, CallbackInfo callback) {
        EventAttackRenderer.invalidateSurface(position);
    }
}
