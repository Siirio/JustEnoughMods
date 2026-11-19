package com.siirio.jemsettlements.network;

import net.minecraft.network.FriendlyByteBuf;

public record FamilyRequestPacket(int entityId) {
    static void encode(FamilyRequestPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.entityId); }
    static FamilyRequestPacket decode(FriendlyByteBuf buffer) { return new FamilyRequestPacket(buffer.readVarInt()); }
}
