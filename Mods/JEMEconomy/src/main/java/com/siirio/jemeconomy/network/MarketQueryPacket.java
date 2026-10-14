package com.siirio.jemeconomy.network;

import net.minecraft.network.FriendlyByteBuf;

public record MarketQueryPacket(String search, String category, int page, boolean open) {
    static void encode(MarketQueryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.search, 64); buffer.writeUtf(packet.category, 16); buffer.writeVarInt(packet.page); buffer.writeBoolean(packet.open);
    }
    static MarketQueryPacket decode(FriendlyByteBuf buffer) {
        return new MarketQueryPacket(buffer.readUtf(64), buffer.readUtf(16), buffer.readVarInt(), buffer.readBoolean());
    }
}
