package com.siirio.jemserver.smp;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class Profiles {
    private final SmpData storage;
    private final java.util.LinkedHashMap<UUID, ProfileState> saved = new java.util.LinkedHashMap<>();
    private java.util.LinkedHashMap<UUID, ProfileState> working;
    private boolean editing;
    Profiles(SmpData storage) { this.storage = storage; }
    void beginEdit() { editing = true; }
    void endEdit() { working = null; editing = false; }
    private java.util.LinkedHashMap<UUID, ProfileState> working() {
        if (!editing) throw new IllegalStateException("Mutation requires an SMP command");
        if (working == null) { working = new java.util.LinkedHashMap<>(); saved.forEach((id, row) -> working.put(id, row.copy())); }
        return working;
    }
    public ProfileState snapshot(UUID id) { var row = (working == null ? saved : working).get(id); return row == null ? null : row.snapshot(); }
    public java.util.List<ProfileState> snapshots() { return (working == null ? saved : working).values().stream().map(ProfileState::snapshot).toList(); }
    public ProfileState current(UUID id) { return working().get(id); }
    public java.util.Collection<ProfileState> currentRows() { return java.util.List.copyOf(working().values()); }
    public ProfileState bind(ProfileState row) {
        if (row == null) return null;
        var current = current(row.id()); SmpRecords.require(current != null, "unavailable"); return current;
    }
    void read(net.minecraft.nbt.ListTag rows) { for (var tag : rows) { var row = ProfileStateNbtCodec.decode((net.minecraft.nbt.CompoundTag) tag); if (row.id() != null) saved.put(row.id(), row); } }
    net.minecraft.nbt.ListTag write() { var rows = new net.minecraft.nbt.ListTag(); saved.values().forEach(row -> rows.add(ProfileStateNbtCodec.encode(row))); return rows; }
    int checkpoint() {
        if (working == null) return 0;
        int changes = 0;
        for (var row : working.values()) {
            var previous = saved.get(row.id());
            if (previous != null && ProfileStateNbtCodec.encode(row).equals(ProfileStateNbtCodec.encode(previous))) continue;
            row.revision(previous == null ? Math.max(1, row.revision() + 1) : previous.revision() + 1);
            saved.put(row.id(), row.copy()); changes++;
        }
        for (UUID id : java.util.List.copyOf(saved.keySet())) if (!working.containsKey(id)) { saved.remove(id); changes++; }
        return changes;
    }
    public void put(ProfileState row) { working().put(row.id(), row); }

    public static ProfileState get(net.minecraft.server.MinecraftServer server, UUID id) {
        var data = SmpData.get(server);
        var profile = data.profiles().current(id);
        if (profile == null) {
            profile = new ProfileState();
            profile.id(id);
            profile.owner(id);
            profile.firstJoin(System.currentTimeMillis());
            data.profiles().put(profile);
        }
        return profile;
    }

    public static ProfileState snapshot(net.minecraft.server.MinecraftServer server, UUID id) {
        ProfileState row = SmpData.get(server).profiles().snapshot(id);
        return row == null ? new ProfileState().snapshot() : row;
    }

    public static void captureProfessions(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> captureProfessionsOwned(player));
    }

    private static void captureProfessionsOwned(ServerPlayer player) {
        if (!DomainFeatures.loaded("jem_professions")) return;
        var snapshot = com.siirio.jemserver.compat.professions.ProfessionHostAdapter.snapshot(player);
        var row = get(player.server, player.getUUID());
        if (!snapshot.equals(row.professions())) {
            row.professions(snapshot);
            
        }
    }

    public static void login(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> loginOwned(player));
    }

    private static void loginOwned(ServerPlayer player) {
        var row = get(player.server, player.getUUID());
        row.name(player.getGameProfile().getName());
        
        captureProfessions(player);
        try {
            LegacyRewards.migrate(player);
        } catch (IllegalStateException | IllegalArgumentException failure) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "jem.smp.escrow_recovery_required"));
            com.mojang.logging.LogUtils.getLogger()
                    .error("SMP escrow login recovery failed for {}", player.getUUID(), failure);
        }
    }

    public static void count(net.minecraft.server.MinecraftServer server, UUID id, ProfileCounter counter) {
        var data = SmpData.get(server);
        data.edit(() -> countOwned(server, id, counter));
    }

    private static void countOwned(net.minecraft.server.MinecraftServer server, UUID id, ProfileCounter counter) {
        var row = get(server, id);
        counter.increment(row);
        
    }

    public static void countEvent(net.minecraft.server.MinecraftServer server, UUID id, String type) {
        var data = SmpData.get(server);
        data.edit(() -> countEventOwned(server, id, type));
    }

    private static void countEventOwned(net.minecraft.server.MinecraftServer server, UUID id, String type) {
        var row = get(server, id);
        row.countEvent(type);
        
    }


}
