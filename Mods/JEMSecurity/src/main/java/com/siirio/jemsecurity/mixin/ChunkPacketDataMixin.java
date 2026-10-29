package com.siirio.jemsecurity.mixin;

import com.siirio.jemsecurity.AntiXray;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ChunkPacketDataMixin {
    @Inject(method = "extractChunkData", at = @At("HEAD"), cancellable = true)
    private static void jemSecurity$filterHiddenOres(FriendlyByteBuf buffer, LevelChunk chunk, CallbackInfo callback) {
        if (AntiXray.writeFilteredChunk(buffer, chunk)) callback.cancel();
    }
}
