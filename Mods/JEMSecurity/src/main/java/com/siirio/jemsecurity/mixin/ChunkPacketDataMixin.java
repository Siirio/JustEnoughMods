package com.siirio.jemsecurity.mixin;

import com.siirio.jemsecurity.AntiXray;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ChunkPacketDataMixin {
    @Unique
    private byte[] jemSecurity$filteredData;

    @Shadow
    private static int calculateChunkSize(LevelChunk chunk) {
        throw new AssertionError();
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;calculateChunkSize(Lnet/minecraft/world/level/chunk/LevelChunk;)I"))
    private int jemSecurity$sizeFilteredData(LevelChunk chunk) {
        int originalSize = calculateChunkSize(chunk);
        jemSecurity$filteredData = AntiXray.encodeFilteredChunk(chunk, originalSize);
        return jemSecurity$filteredData == null ? originalSize : jemSecurity$filteredData.length;
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;extractChunkData(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    private void jemSecurity$writeFilteredData(FriendlyByteBuf buffer, LevelChunk chunk) {
        if (jemSecurity$filteredData == null) {
            ClientboundLevelChunkPacketData.extractChunkData(buffer, chunk);
            return;
        }
        buffer.writeBytes(jemSecurity$filteredData);
        jemSecurity$filteredData = null;
    }
}
