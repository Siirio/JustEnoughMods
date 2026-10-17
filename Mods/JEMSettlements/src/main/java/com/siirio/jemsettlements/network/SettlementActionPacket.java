package com.siirio.jemsettlements.network;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** A bounded intent. Prices, ownership and successful outcomes are never client supplied. */
public record SettlementActionPacket(String action, UUID townId, UUID requestId, CompoundTag payload) {
    static void encode(SettlementActionPacket packet, FriendlyByteBuf out) {
        out.writeUtf(packet.action, 24); out.writeBoolean(packet.townId != null);
        if (packet.townId != null) out.writeUUID(packet.townId);
        out.writeUUID(packet.requestId); out.writeNbt(packet.payload);
    }
    static SettlementActionPacket decode(FriendlyByteBuf in) {
        if (in.readableBytes() > 4096) throw new io.netty.handler.codec.DecoderException("Settlement intent exceeds budget");
        String action = in.readUtf(24); UUID town = in.readBoolean() ? in.readUUID() : null;
        UUID request = in.readUUID(); CompoundTag payload = in.readNbt();
        if (in.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing settlement intent");
        return new SettlementActionPacket(action, town, request, payload == null ? new CompoundTag() : payload);
    }
}
