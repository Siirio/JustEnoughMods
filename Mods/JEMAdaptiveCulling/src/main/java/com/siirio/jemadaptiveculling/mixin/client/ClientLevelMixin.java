package com.siirio.jemadaptiveculling.mixin.client;

import com.siirio.jemadaptiveculling.client.GeometryTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void jemadaptiveculling$trackGeometryChange(BlockPos position, BlockState state, int flags, int recursion,
                                                        CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            GeometryTracker.changed();
        }
    }
    @Inject(method = "unload", at = @At("HEAD"))
    private void jemadaptiveculling$trackChunkUnload(LevelChunk chunk, CallbackInfo callback) {
        GeometryTracker.changed();
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    private void jemadaptiveculling$trackChunkLoad(ChunkPos position, CallbackInfo callback) {
        GeometryTracker.changed();
    }
    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void jemadaptiveculling$trackServerGeometry(BlockPos position, BlockState state, int flags, CallbackInfo callback) {
        GeometryTracker.changed();
    }
}
