package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Stable physical tool lineage across native durability changes, without ordinary item tags. */
public final class WildlifeQuestTools extends SavedData {
    private final Map<UUID, UUID> origins = new LinkedHashMap<>();
    private final Map<UUID, ItemStack> latest = new LinkedHashMap<>();
    private NativeFoodSources physical;
    private static final int LIMIT = 16384;
    public static WildlifeQuestTools get(ServerLevel level) {
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(WildlifeQuestTools::load, WildlifeQuestTools::new, "jem_wildlife_tools_v4");
        data.physical = NativeFoodSources.get(level);
        return data;
    }
    public void record(UUID batch, UUID origin, ItemStack actual) {
        if (batch != null && origin != null && !actual.isEmpty()) {
            if (!origins.containsKey(batch) && origins.size() >= LIMIT) {
                UUID revoked = origins.keySet().iterator().next();
                UUID previous = origins.remove(revoked);
                if (physical != null) physical.activate(revoked, false);
                if (!origins.containsValue(previous)) latest.remove(previous);
            }
            origins.put(batch, origin); ItemStack snapshot = actual.copyWithCount(1);
            NativeFoodSources.claims(snapshot, List.of(new NativeFoodSources.Claim(batch, 1))); latest.put(origin, snapshot); setDirty();
        }
    }
    public UUID origin(ItemStack stack) {
        for (var claim : NativeFoodSources.claims(stack)) if (origins.containsKey(claim.batch())) return origins.get(claim.batch());
        return null;
    }
    public void retired(ItemStack before) {
        var retired = new java.util.HashSet<UUID>();
        for (var claim : NativeFoodSources.claims(before)) { UUID origin = origins.remove(claim.batch()); if (origin != null) retired.add(origin); }
        for (UUID origin : retired) if (!origins.containsValue(origin)) latest.remove(origin); setDirty();
    }
    public static boolean consume(ServerPlayer actor, String kind, UUID origin) {
        if (origin == null) return false; var physical = NativeFoodSources.get(actor.serverLevel()); var lineage = get(actor.serverLevel());
        ItemStack receipt = lineage.latest.get(origin);
        if (receipt != null && physical.consume(actor.getUUID(), receipt, kind, 1)) {
            lineage.origins.entrySet().removeIf(row -> row.getValue().equals(origin)); lineage.latest.remove(origin); lineage.setDirty(); return true;
        }
        return false;
    }
    private static WildlifeQuestTools load(CompoundTag tag) {
        var data = new WildlifeQuestTools(); ListTag rows = tag.getList("origins", 10);
        for (int i = 0; i < Math.min(LIMIT, rows.size()); i++) {
            var row = rows.getCompound(i);
            if (row.hasUUID("batch") && row.hasUUID("origin")) {
                data.origins.put(row.getUUID("batch"), row.getUUID("origin"));
                ItemStack receipt = ItemStack.of(row.getCompound("receipt")); if (!receipt.isEmpty()) data.latest.put(row.getUUID("origin"), receipt);
            }
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag(); origins.forEach((batch, origin) -> {
            CompoundTag row = new CompoundTag(); row.putUUID("batch", batch); row.putUUID("origin", origin);
            ItemStack receipt = latest.get(origin); if (receipt != null) row.put("receipt", receipt.save(new CompoundTag())); rows.add(row);
        }); tag.put("origins", rows); return tag;
    }
}
