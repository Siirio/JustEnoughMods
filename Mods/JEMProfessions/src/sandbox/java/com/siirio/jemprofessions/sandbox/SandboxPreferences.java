package com.siirio.jemprofessions.sandbox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Action selection only; XP, points and unlocks always remain in native Skills data. */
public final class SandboxPreferences extends SavedData {
    private final Map<UUID, String> selected = new HashMap<>();
    public static SandboxPreferences get(ServerPlayer player) {
        return player.getServer().overworld().getDataStorage().computeIfAbsent(SandboxPreferences::load, SandboxPreferences::new, "jem_profession_controls");
    }
    public String selected(ServerPlayer player) { return selected.getOrDefault(player.getUUID(), ""); }
    public void selected(ServerPlayer player, String node) { selected.put(player.getUUID(), node); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag choices = new CompoundTag(); selected.forEach((id, node) -> choices.putString(id.toString(), node)); tag.put("Selected", choices); return tag;
    }
    private static SandboxPreferences load(CompoundTag tag) {
        SandboxPreferences data = new SandboxPreferences(); CompoundTag choices = tag.getCompound("Selected");
        for (String id : choices.getAllKeys()) {
            try { String node = choices.getString(id); if (node.length() <= 64) data.selected.put(UUID.fromString(id), node); }
            catch (IllegalArgumentException ignored) { }
        }
        return data;
    }
}
