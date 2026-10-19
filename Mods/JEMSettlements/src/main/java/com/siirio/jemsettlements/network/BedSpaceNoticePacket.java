package com.siirio.jemsettlements.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
public record BedSpaceNoticePacket(int entityId, UUID entityUuid) {
    static void encode(BedSpaceNoticePacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.entityId); buffer.writeUUID(packet.entityUuid); }
    static BedSpaceNoticePacket decode(FriendlyByteBuf buffer) { return new BedSpaceNoticePacket(buffer.readVarInt(), buffer.readUUID()); }
}
