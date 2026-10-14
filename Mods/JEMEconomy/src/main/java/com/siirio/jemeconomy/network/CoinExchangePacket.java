package com.siirio.jemeconomy.network;

import com.siirio.jemeconomy.physical.CoinDenomination;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record CoinExchangePacket(CoinDenomination source, int sourceCount, CoinDenomination target, UUID requestId,
                                 String search, String category, int page) {
    static void encode(CoinExchangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.source); buffer.writeVarInt(packet.sourceCount); buffer.writeEnum(packet.target); buffer.writeUUID(packet.requestId);
        buffer.writeUtf(packet.search, 64); buffer.writeUtf(packet.category, 16); buffer.writeVarInt(packet.page);
    }
    static CoinExchangePacket decode(FriendlyByteBuf buffer) {
        return new CoinExchangePacket(buffer.readEnum(CoinDenomination.class), buffer.readVarInt(), buffer.readEnum(CoinDenomination.class), buffer.readUUID(),
                buffer.readUtf(64), buffer.readUtf(16), buffer.readVarInt());
    }
}
