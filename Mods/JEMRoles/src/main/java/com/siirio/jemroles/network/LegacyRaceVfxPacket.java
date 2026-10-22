package com.siirio.jemroles.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** Receive-only v4 discriminator 3: consumes retired VFX without retaining its renderer graph. */
final class LegacyRaceVfxPacket {
    private static final LegacyRaceVfxPacket INSTANCE = new LegacyRaceVfxPacket();
    private LegacyRaceVfxPacket() {}

    static LegacyRaceVfxPacket decode(FriendlyByteBuf buffer) {
        buffer.readVarInt();
        buffer.readUtf(128);
        readPosition(buffer);
        readPosition(buffer);
        buffer.readVarInt();
        buffer.readVarInt();
        int related = count(buffer, 8);
        for (int i = 0; i < related; i++) buffer.readVarInt();
        int points = count(buffer, 16);
        for (int i = 0; i < points; i++) readPosition(buffer);
        buffer.readVarInt();
        buffer.readVarInt();
        buffer.readLong();
        return INSTANCE;
    }

    static void encode(LegacyRaceVfxPacket packet, FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("Retired race VFX has no producer");
    }

    private static int count(FriendlyByteBuf buffer, int maximum) {
        int value = buffer.readVarInt();
        if (value < 0 || value > maximum) throw new DecoderException("Invalid legacy VFX count");
        return value;
    }

    private static void readPosition(FriendlyByteBuf buffer) {
        buffer.readDouble();
        buffer.readDouble();
        buffer.readDouble();
    }
}
