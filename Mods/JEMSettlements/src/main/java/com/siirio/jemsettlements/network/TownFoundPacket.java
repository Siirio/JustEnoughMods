package com.siirio.jemsettlements.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record TownFoundPacket(String name, BlockPos bell) {
    static void encode(TownFoundPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.name, 32); buffer.writeBlockPos(packet.bell); }
    static TownFoundPacket decode(FriendlyByteBuf buffer) { return new TownFoundPacket(buffer.readUtf(32), buffer.readBlockPos()); }
}
