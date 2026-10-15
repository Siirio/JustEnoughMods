package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.progress.ProfessionId;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

public record ProfessionStatusPacket(boolean open, boolean authenticated, long[] xp, int[] levels, int[] points, Set<String> unlocked) {
    public ProfessionStatusPacket {
        xp = xp.clone(); levels = levels.clone(); points = points.clone(); unlocked = Set.copyOf(unlocked);
    }
    static void encode(ProfessionStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open); buffer.writeBoolean(packet.authenticated);
        buffer.writeVarInt(ProfessionId.values().length);
        for (int i = 0; i < ProfessionId.values().length; i++) {
            buffer.writeVarLong(packet.xp[i]); buffer.writeVarInt(packet.levels[i]); buffer.writeVarInt(packet.points[i]);
        }
        buffer.writeVarInt(packet.unlocked.size());
        packet.unlocked.forEach(value -> buffer.writeUtf(value, 96));
    }
    static ProfessionStatusPacket decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean(), authenticated = buffer.readBoolean();
        int declared = Math.min(ProfessionId.values().length, buffer.readVarInt());
        long[] xp = new long[ProfessionId.values().length]; int[] levels = new int[xp.length], points = new int[xp.length];
        for (int i = 0; i < declared; i++) { xp[i] = buffer.readVarLong(); levels[i] = buffer.readVarInt(); points[i] = buffer.readVarInt(); }
        int count = Math.min(256, buffer.readVarInt()); Set<String> unlocked = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) unlocked.add(buffer.readUtf(96));
        return new ProfessionStatusPacket(open, authenticated, xp, levels, points, unlocked);
    }
    @Override public long[] xp() { return xp.clone(); }
    @Override public int[] levels() { return levels.clone(); }
    @Override public int[] points() { return points.clone(); }
    @Override public Set<String> unlocked() { return Set.copyOf(unlocked); }
}
