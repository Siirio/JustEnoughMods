package com.siirio.jemeconomy.network;

import net.minecraft.network.FriendlyByteBuf;

public record WalletStatusPacket(boolean open, long balance, long heldValue, int[] counts, int invalid, long outstanding) {
    public WalletStatusPacket { counts = counts.clone(); }
    static void encode(WalletStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open); buffer.writeVarLong(packet.balance); buffer.writeVarLong(packet.heldValue);
        for (int i = 0; i < 5; i++) buffer.writeVarInt(i < packet.counts.length ? packet.counts[i] : 0);
        buffer.writeVarInt(packet.invalid); buffer.writeVarLong(packet.outstanding);
    }
    static WalletStatusPacket decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean(); long balance = buffer.readVarLong(); long held = buffer.readVarLong();
        int[] counts = new int[5]; for (int i = 0; i < 5; i++) counts[i] = buffer.readVarInt();
        return new WalletStatusPacket(open, balance, held, counts, buffer.readVarInt(), buffer.readVarLong());
    }
    @Override public int[] counts() { return counts.clone(); }
}
