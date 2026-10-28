package com.justenoughmods.achievementguide.integration.battle;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Actual equipment output/action lineage, kept outside native item tags. Native wear may change Damage. */
public final class WorldBattleEquipmentOrigins extends SavedData {
    private record Origin(UUID actor, String kind, ItemStack variant) {}
    private final Map<UUID, Origin> origins = new LinkedHashMap<>();
    private static WorldBattleEquipmentOrigins get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(WorldBattleEquipmentOrigins::load,
                WorldBattleEquipmentOrigins::new, "jem_battle_equipment_origins_v4");
    }
    public static void confirmed(ServerPlayer actor, UUID batch, String kind, ItemStack original) {
        WorldBattleEquipmentOrigins data = get(actor.serverLevel());
        if (batch == null || original.isEmpty()) return;
        if (data.origins.size() >= 16384 && !data.origins.containsKey(batch)) {
            UUID oldest = data.origins.keySet().iterator().next(); data.origins.remove(oldest);
            NativeFoodSources.get(actor.serverLevel()).activate(oldest, false);
        }
        ItemStack variant = original.copyWithCount(1); NativeFoodSources.claims(variant, List.of());
        data.origins.put(batch, new Origin(actor.getUUID(), kind, variant)); data.setDirty();
    }
    public static boolean has(ServerPlayer actor, ItemStack actual, String kind) {
        if (!BattleQuestProof.real(actor) || actual.isEmpty() || actual.getCount() != 1) return false;
        WorldBattleEquipmentOrigins data = get(actor.serverLevel());
        for (var claim : NativeFoodSources.claims(actual)) {
            Origin origin = data.origins.get(claim.batch());
            if (claim.count() != 1 || origin == null || !origin.actor.equals(actor.getUUID()) || !origin.kind.equals(kind)
                    || actual.getItem() != origin.variant.getItem()) continue;
            ItemStack original = origin.variant.copy(); NativeFoodSources.claims(original, List.of(claim));
            if (actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(original, kind))) return true;
        }
        return false;
    }
    public static boolean mode(ServerPlayer actor, ItemStack actual, String family, int mode) {
        if (!BattleQuestProof.real(actor) || mode < 1 || mode > 2 || actual.getCount() != 1) return false;
        String kind = "battle_mode_" + family + "_" + mode;
        if (has(actor, actual, "battle_mode_" + family + "_" + (3 - mode))) return true;
        WorldBattleEquipmentOrigins data = get(actor.serverLevel());
        java.util.Set<UUID> replaced = new java.util.HashSet<>();
        for (var claim : NativeFoodSources.claims(actual)) {
            Origin origin = data.origins.get(claim.batch());
            if (origin != null && origin.kind.startsWith("battle_mode_" + family + "_") && !origin.actor.equals(actor.getUUID())) {
                replaced.add(claim.batch()); data.origins.remove(claim.batch());
                NativeFoodSources.get(actor.serverLevel()).activate(claim.batch(), false);
            }
        }
        if (!replaced.isEmpty()) {
            NativeFoodSources.claims(actual, NativeFoodSources.claims(actual).stream().filter(c -> !replaced.contains(c.batch())).toList()); data.setDirty();
        }
        if (!has(actor, actual, kind)) {
            UUID batch = NativeFoodSources.get(actor.serverLevel()).certify(actor.serverLevel(), actor.getUUID(), kind, actual, 1);
            confirmed(actor, batch, kind, actual);
        }
        return false;
    }
    private static WorldBattleEquipmentOrigins load(CompoundTag tag) {
        WorldBattleEquipmentOrigins data = new WorldBattleEquipmentOrigins();
        var rows = tag.getList("origins", 10);
        for (int i = 0; i < Math.min(rows.size(), 16384); i++) {
            var row = rows.getCompound(i);
            if (row.hasUUID("batch") && row.hasUUID("actor")) data.origins.put(row.getUUID("batch"),
                    new Origin(row.getUUID("actor"), row.getString("kind"), ItemStack.of(row.getCompound("variant"))));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var rows = new net.minecraft.nbt.ListTag();
        origins.forEach((batch, origin) -> {
            var row = new CompoundTag(); row.putUUID("batch", batch); row.putUUID("actor", origin.actor);
            row.putString("kind", origin.kind); row.put("variant", origin.variant.save(new CompoundTag())); rows.add(row);
        });
        tag.put("origins", rows); return tag;
    }
}
