package com.siirio.jemprofessions.network;

import net.minecraft.network.FriendlyByteBuf;

public record ArchitectHotbarSlotPacket(int logicalSlot) {
    static void encode(ArchitectHotbarSlotPacket packet, FriendlyByteBuf buffer) { buffer.writeByte(packet.logicalSlot); }
    static ArchitectHotbarSlotPacket decode(FriendlyByteBuf buffer) { return new ArchitectHotbarSlotPacket(buffer.readUnsignedByte()); }
}
