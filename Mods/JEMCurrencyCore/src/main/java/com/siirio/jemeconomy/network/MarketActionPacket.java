package com.siirio.jemeconomy.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MarketActionPacket(Action action, ResourceLocation item, int units, long revision, UUID requestId,
                                 String search, String category, int page) {
    public enum Action { BUY, SELL }
    static void encode(MarketActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action); buffer.writeResourceLocation(packet.item); buffer.writeVarInt(packet.units);
        buffer.writeVarLong(packet.revision); buffer.writeUUID(packet.requestId); buffer.writeUtf(packet.search, 64);
        buffer.writeUtf(packet.category, 16); buffer.writeVarInt(packet.page);
    }
    static MarketActionPacket decode(FriendlyByteBuf buffer) {
        return new MarketActionPacket(buffer.readEnum(Action.class), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readVarLong(), buffer.readUUID(), buffer.readUtf(64), buffer.readUtf(16), buffer.readVarInt());
    }
}
