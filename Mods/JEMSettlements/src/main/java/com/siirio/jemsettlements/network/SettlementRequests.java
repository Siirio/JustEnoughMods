package com.siirio.jemsettlements.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent intent/result journal; request IDs cannot be rebound after a reconnect or restart. */
final class SettlementRequests extends SavedData {
    private static final int LIMIT = 65_536;
    private final Map<String, CompoundTag> operations = new LinkedHashMap<>();
    static SettlementRequests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SettlementRequests::load, SettlementRequests::new, "jem_settlement_requests");
    }
    CompoundTag prior(UUID actor, UUID request) { return operations.get(actor + ":" + request); }
    int uncertain(UUID actor) {
        String prefix = actor + ":"; int count = 0;
        for (var entry : operations.entrySet()) if (entry.getKey().startsWith(prefix) && !entry.getValue().contains("Result")) count++;
        return count;
    }
    boolean admit(UUID actor, SettlementActionPacket packet, java.util.Set<Long> activeSessions) {
        String key = actor + ":" + packet.requestId();
        CompoundTag prior = operations.get(key);
        if (prior != null) return prior.getString("Terms").equals(terms(packet));
        if (operations.size() >= LIMIT) {
            var iterator = operations.entrySet().iterator();
            while (iterator.hasNext() && operations.size() >= LIMIT) {
                CompoundTag row = iterator.next().getValue();
                if (row.contains("Result") && !activeSessions.contains(row.getLong("SessionPrefix"))) iterator.remove();
            }
            if (operations.size() >= LIMIT) return false;
        }
        CompoundTag value = new CompoundTag(); value.putString("Terms", terms(packet)); value.putString("Action", packet.action());
        if (packet.townId() != null) value.putUUID("Town", packet.townId());
        value.putLong("SessionPrefix", packet.requestId().getMostSignificantBits());
        operations.put(key, value); setDirty(); return true;
    }
    void finish(UUID actor, UUID request, String result, UUID town, long gameTime) {
        CompoundTag row = prior(actor, request); if (row == null) return;
        row.putString("Result", result); if (town != null) row.putUUID("Town", town);
        row.putLong("GameTime", gameTime); setDirty();
    }
    net.minecraft.nbt.ListTag history(UUID town) {
        var rows = new java.util.ArrayList<CompoundTag>();
        for (CompoundTag row : operations.values()) if (row.hasUUID("Town") && row.getUUID("Town").equals(town)
                && (row.getString("Result").equals("ACCEPTED") || row.getString("Result").equals("REPLAYED"))) rows.add(row.copy());
        var result = new net.minecraft.nbt.ListTag();
        for (int i = Math.max(0, rows.size() - 16); i < rows.size(); i++) result.add(rows.get(i));
        return result;
    }
    static String terms(SettlementActionPacket packet) {
        StringBuilder text = new StringBuilder(packet.action()).append(':').append(packet.townId());
        packet.payload().getAllKeys().stream().filter(key -> !key.equals("Session")).sorted().forEach(key -> text.append('|').append(key).append('=').append(packet.payload().get(key)));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag(); operations.forEach(rows::put); tag.put("Operations", rows); return tag;
    }
    private static SettlementRequests load(CompoundTag tag) {
        SettlementRequests data = new SettlementRequests(); CompoundTag rows = tag.getCompound("Operations");
        for (String key : rows.getAllKeys()) data.operations.put(key, rows.getCompound(key));
        return data;
    }
}
