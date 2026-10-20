package com.siirio.jemeconomy.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import com.siirio.jemeconomy.physical.CoinDenomination;

public record ContainerCurrencyPacket(int containerId, Action action, long amount, CoinDenomination source,
                                      int count, CoinDenomination target, UUID requestId) {
    public enum Action { SNAPSHOT, WITHDRAW, EXCHANGE }
    static void encode(ContainerCurrencyPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.containerId); b.writeEnum(p.action); b.writeVarLong(p.amount);
        b.writeEnum(p.source); b.writeVarInt(p.count); b.writeEnum(p.target); b.writeUUID(p.requestId);
    }
    static ContainerCurrencyPacket decode(FriendlyByteBuf b) {
        return new ContainerCurrencyPacket(b.readVarInt(), b.readEnum(Action.class), b.readVarLong(),
                b.readEnum(CoinDenomination.class), b.readVarInt(), b.readEnum(CoinDenomination.class), b.readUUID());
    }
}
