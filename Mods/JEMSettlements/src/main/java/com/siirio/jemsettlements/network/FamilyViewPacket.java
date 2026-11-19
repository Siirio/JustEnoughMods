package com.siirio.jemsettlements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record FamilyViewPacket(int entityId, boolean open, CompoundTag view) {
    static void encode(FamilyViewPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.entityId); buffer.writeBoolean(packet.open); buffer.writeNbt(packet.view); }
    static FamilyViewPacket decode(FriendlyByteBuf buffer) { return new FamilyViewPacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readNbt()); }
}
