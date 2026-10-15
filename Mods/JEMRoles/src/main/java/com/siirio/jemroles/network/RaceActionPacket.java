package com.siirio.jemroles.network;

import net.minecraft.network.FriendlyByteBuf;

public record RaceActionPacket(Action action) {
    public enum Action { SIGNATURE, UTILITY_PRESS, UTILITY_RELEASE }

    static void encode(RaceActionPacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.action); }
    static RaceActionPacket decode(FriendlyByteBuf buffer) { return new RaceActionPacket(buffer.readEnum(Action.class)); }
}
