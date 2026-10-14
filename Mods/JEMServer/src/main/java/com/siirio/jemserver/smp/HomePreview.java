package com.siirio.jemserver.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HomePreview(ResourceLocation dimension, BlockPos position) {
    static void encode(HomePreview packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension); buffer.writeBlockPos(packet.position);
    }
    static HomePreview decode(FriendlyByteBuf buffer) {
        return new HomePreview(buffer.readResourceLocation(), buffer.readBlockPos());
    }
}
