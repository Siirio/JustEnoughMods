package com.siirio.jemeconomy.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record ContainerCurrencyStatusPacket(int containerId, UUID requestId, long wallet, String result) {
    static void encode(ContainerCurrencyStatusPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.containerId); b.writeUUID(p.requestId); b.writeVarLong(p.wallet); b.writeUtf(p.result, 64);
    }
    static ContainerCurrencyStatusPacket decode(FriendlyByteBuf b) {
        return new ContainerCurrencyStatusPacket(b.readVarInt(), b.readUUID(), b.readVarLong(), b.readUtf(64));
    }
}
