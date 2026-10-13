package com.siirio.jemeconomy.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record WalletActionPacket(Action action, long amount, UUID requestId) {
    public enum Action { WITHDRAW, DEPOSIT_ALL }
    static void encode(WalletActionPacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.action); buffer.writeVarLong(packet.amount); buffer.writeUUID(packet.requestId); }
    static WalletActionPacket decode(FriendlyByteBuf buffer) { return new WalletActionPacket(buffer.readEnum(Action.class), buffer.readVarLong(), buffer.readUUID()); }
}
