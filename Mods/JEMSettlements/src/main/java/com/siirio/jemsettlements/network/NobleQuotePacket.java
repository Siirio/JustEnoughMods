package com.siirio.jemsettlements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record NobleQuotePacket(CompoundTag view) {
    static void encode(NobleQuotePacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.view); }
    static NobleQuotePacket decode(FriendlyByteBuf buffer) { CompoundTag view = buffer.readNbt(); return new NobleQuotePacket(view == null ? new CompoundTag() : view); }
}
