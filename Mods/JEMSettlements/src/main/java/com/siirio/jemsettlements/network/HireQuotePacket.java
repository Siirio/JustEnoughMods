package com.siirio.jemsettlements.network;

import net.minecraft.network.FriendlyByteBuf;

public record HireQuotePacket(int entityId, long cost, long wage) {
    static void encode(HireQuotePacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.entityId); buffer.writeLong(packet.cost); buffer.writeLong(packet.wage); }
    static HireQuotePacket decode(FriendlyByteBuf buffer) { return new HireQuotePacket(buffer.readVarInt(), buffer.readLong(), buffer.readLong()); }
}
