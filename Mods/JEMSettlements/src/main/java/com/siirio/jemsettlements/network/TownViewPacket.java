package com.siirio.jemsettlements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record TownViewPacket(boolean open, CompoundTag view) {
    static void encode(TownViewPacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.open); buffer.writeNbt(packet.view); }
    static TownViewPacket decode(FriendlyByteBuf buffer) { boolean open = buffer.readBoolean(); CompoundTag tag = buffer.readNbt(); return new TownViewPacket(open, tag == null ? new CompoundTag() : tag); }
}
