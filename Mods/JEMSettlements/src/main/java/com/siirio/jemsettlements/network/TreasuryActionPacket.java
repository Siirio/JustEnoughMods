package com.siirio.jemsettlements.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record TreasuryActionPacket(boolean deposit, long amount, UUID requestId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(deposit);
        buffer.writeVarLong(amount);
        buffer.writeUUID(requestId);
    }

    public static TreasuryActionPacket decode(FriendlyByteBuf buffer) {
        return new TreasuryActionPacket(buffer.readBoolean(), buffer.readVarLong(), buffer.readUUID());
    }
}
