package com.siirio.jemeconomy.network;

import net.minecraft.network.FriendlyByteBuf;

public record WalletStatusPacket(boolean open, long balance, long heldValue, int[] counts, int invalid, long outstanding,
                                 String primaryDemand, String secondaryDemand, long ticksUntilUpdate, long accountRemaining, long globalRemaining) {
    public WalletStatusPacket { counts = counts.clone(); }
    static void encode(WalletStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open); buffer.writeVarLong(packet.balance); buffer.writeVarLong(packet.heldValue);
        for (int i = 0; i < 5; i++) buffer.writeVarInt(i < packet.counts.length ? packet.counts[i] : 0);
        buffer.writeVarInt(packet.invalid); buffer.writeVarLong(packet.outstanding); buffer.writeUtf(packet.primaryDemand, 32); buffer.writeUtf(packet.secondaryDemand, 32);
        buffer.writeVarLong(packet.ticksUntilUpdate); buffer.writeVarLong(packet.accountRemaining); buffer.writeVarLong(packet.globalRemaining);
    }
    static WalletStatusPacket decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean(); long balance = buffer.readVarLong(); long held = buffer.readVarLong();
        int[] counts = new int[5]; for (int i = 0; i < 5; i++) counts[i] = buffer.readVarInt();
        return new WalletStatusPacket(open, balance, held, counts, buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf(32), buffer.readUtf(32),
                buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong());
    }
    @Override public int[] counts() { return counts.clone(); }
}
