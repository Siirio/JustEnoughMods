package com.siirio.jemeconomy.network;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record ContainerCurrencyStatusPacket(int containerId, UUID requestId, long wallet, String result,
                                            List<Integer> heldCounts, List<Integer> exchangeCounts) {
    public ContainerCurrencyStatusPacket {
        heldCounts = List.copyOf(heldCounts);
        exchangeCounts = List.copyOf(exchangeCounts);
    }
    static void encode(ContainerCurrencyStatusPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.containerId); b.writeUUID(p.requestId); b.writeVarLong(p.wallet); b.writeUtf(p.result, 64);
        for (int count : p.heldCounts) b.writeVarInt(count);
        for (int count : p.exchangeCounts) b.writeVarInt(count);
    }
    static ContainerCurrencyStatusPacket decode(FriendlyByteBuf b) {
        int containerId = b.readVarInt(); UUID requestId = b.readUUID(); long wallet = b.readVarLong(); String result = b.readUtf(64);
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < com.siirio.jemeconomy.physical.CoinDenomination.values().length; i++) counts.add(b.readVarInt());
        List<Integer> exchange = new ArrayList<>();
        for (int i = 0; i < com.siirio.jemeconomy.physical.CoinDenomination.values().length; i++) exchange.add(b.readVarInt());
        return new ContainerCurrencyStatusPacket(containerId, requestId, wallet, result, counts, exchange);
    }

    public int held(com.siirio.jemeconomy.physical.CoinDenomination denomination) {
        return denomination.ordinal() < heldCounts.size() ? heldCounts.get(denomination.ordinal()) : 0;
    }

    public int exchangeable(com.siirio.jemeconomy.physical.CoinDenomination denomination) {
        return denomination.ordinal() < exchangeCounts.size() ? exchangeCounts.get(denomination.ordinal()) : 0;
    }
}
