package com.siirio.jemprofessions.network;

import java.util.Set;
import java.util.LinkedHashSet;
import net.minecraft.network.FriendlyByteBuf;

public record ProfessionStatusPacket(boolean open, boolean authenticated, long[] xp, Set<String> enabled) {
    public ProfessionStatusPacket {
        xp = xp.clone();
        enabled = Set.copyOf(enabled);
    }
    static void encode(ProfessionStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open);
        buffer.writeBoolean(packet.authenticated);
        buffer.writeVarInt(packet.xp.length);
        for (long value : packet.xp) buffer.writeVarLong(value);
        buffer.writeVarInt(packet.enabled.size());
        packet.enabled.forEach(value -> buffer.writeUtf(value, 96));
    }
    static ProfessionStatusPacket decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean();
        boolean authenticated = buffer.readBoolean();
        int size = Math.min(10, buffer.readVarInt());
        long[] xp = new long[10];
        for (int i = 0; i < size; i++) xp[i] = buffer.readVarLong();
        int enabledSize = Math.min(256, buffer.readVarInt());
        Set<String> enabled = new LinkedHashSet<>();
        for (int i = 0; i < enabledSize; i++) enabled.add(buffer.readUtf(96));
        return new ProfessionStatusPacket(open, authenticated, xp, enabled);
    }
    @Override public long[] xp() { return xp.clone(); }
    @Override public Set<String> enabled() { return Set.copyOf(enabled); }
}
