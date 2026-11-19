package com.siirio.jemsettlements.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record FamilyComplaintPacket(int entityId, UUID entityUuid, String reason) {
    static void encode(FamilyComplaintPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.entityId); buffer.writeUUID(packet.entityUuid); buffer.writeUtf(packet.reason, 32); }
    static FamilyComplaintPacket decode(FriendlyByteBuf buffer) { return new FamilyComplaintPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(32)); }
}
