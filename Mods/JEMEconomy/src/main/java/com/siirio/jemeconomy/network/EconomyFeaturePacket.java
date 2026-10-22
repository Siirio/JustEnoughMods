package com.siirio.jemeconomy.network;

import net.minecraft.network.FriendlyByteBuf;

/** Host-owned feature state; never lets a client enable currency. */
public record EconomyFeaturePacket(boolean enabled) {
    public static void encode(EconomyFeaturePacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.enabled()); }
    public static EconomyFeaturePacket decode(FriendlyByteBuf buffer) { return new EconomyFeaturePacket(buffer.readBoolean()); }
}
