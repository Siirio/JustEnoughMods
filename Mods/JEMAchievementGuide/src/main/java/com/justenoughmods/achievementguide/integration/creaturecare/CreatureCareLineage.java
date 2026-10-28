package com.justenoughmods.achievementguide.integration.creaturecare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/** Bounded physical pot and placed-egg lineage, separate from ordinary item NBT. */
public final class CreatureCareLineage extends SavedData {
    public record Egg(UUID actor, UUID source) {}
    private final Map<UUID, CompoundTag> mice = new LinkedHashMap<>();
    private final Map<String, List<Egg>> eggs = new LinkedHashMap<>();
    private static final int LIMIT = 8192;
    public static CreatureCareLineage get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(CreatureCareLineage::load,
                CreatureCareLineage::new, "jem_creature_care_lineage_v4");
    }
    public static CompoundTag nativeState(Entity entity) {
        CompoundTag state = new CompoundTag(); entity.saveWithoutId(state);
        // A native new entity keeps its own identity and placement. Every non-spatial native field must match.
        for (String key : List.of("UUID", "Pos", "Motion", "Rotation", "OnGround", "FallDistance")) state.remove(key);
        return state;
    }
    public void mouse(UUID batch, CompoundTag state) {
        if (batch != null) {
            if (!mice.containsKey(batch) && mice.size() >= LIMIT) mice.remove(mice.keySet().iterator().next());
            mice.put(batch, state.copy()); setDirty();
        }
    }
    public CompoundTag mouse(UUID batch) { return mice.get(batch); }
    public void usedMouse(UUID batch) { mice.remove(batch); setDirty(); }
    private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "/" + pos.asLong(); }
    public List<Egg> eggs(ServerLevel level, BlockPos pos) { return eggs.getOrDefault(key(level, pos), List.of()); }
    public List<Egg> eggs(ServerLevel level, BlockPos pos, int nativeCount) {
        List<Egg> known = eggs(level, pos);
        if (known.size() == nativeCount) return known;
        List<Egg> unknown = new ArrayList<>();
        for (int i = 0; i < Math.min(2, nativeCount); i++) unknown.add(new Egg(null, null));
        return unknown;
    }
    public void eggs(ServerLevel level, BlockPos pos, List<Egg> value) {
        String key = key(level, pos);
        if (value.isEmpty()) eggs.remove(key);
        else {
            if (!eggs.containsKey(key) && eggs.size() >= LIMIT) eggs.remove(eggs.keySet().iterator().next());
            eggs.put(key, List.copyOf(value.subList(0, Math.min(2, value.size()))));
        }
        setDirty();
    }
    private static CreatureCareLineage load(CompoundTag tag) {
        CreatureCareLineage result = new CreatureCareLineage();
        ListTag mice = tag.getList("mice", 10);
        for (int i = 0; i < Math.min(LIMIT, mice.size()); i++) {
            CompoundTag row = mice.getCompound(i);
            if (row.hasUUID("batch")) result.mice.put(row.getUUID("batch"), row.getCompound("state").copy());
        }
        ListTag eggs = tag.getList("eggs", 10);
        for (int i = 0; i < Math.min(LIMIT, eggs.size()); i++) {
            CompoundTag row = eggs.getCompound(i); ListTag sources = row.getList("sources", 10);
            List<Egg> value = new ArrayList<>();
            for (int j = 0; j < Math.min(2, sources.size()); j++) {
                CompoundTag source = sources.getCompound(j);
                value.add(source.hasUUID("actor") && source.hasUUID("source") ? new Egg(source.getUUID("actor"), source.getUUID("source")) : new Egg(null, null));
            }
            if (!value.isEmpty()) result.eggs.put(row.getString("pos"), value);
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag mouseRows = new ListTag();
        mice.forEach((id, state) -> { CompoundTag row = new CompoundTag(); row.putUUID("batch", id); row.put("state", state.copy()); mouseRows.add(row); });
        tag.put("mice", mouseRows); ListTag eggRows = new ListTag();
        eggs.forEach((pos, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("pos", pos); ListTag sources = new ListTag();
            value.forEach(egg -> {
                CompoundTag source = new CompoundTag();
                if (egg.actor != null && egg.source != null) { source.putUUID("actor", egg.actor); source.putUUID("source", egg.source); }
                sources.add(source);
            });
            row.put("sources", sources); eggRows.add(row);
        });
        tag.put("eggs", eggRows); return tag;
    }
}
