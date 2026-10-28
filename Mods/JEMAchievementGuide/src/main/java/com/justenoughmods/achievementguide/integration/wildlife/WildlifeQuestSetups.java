package com.justenoughmods.achievementguide.integration.wildlife;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded native setup ownership, never a player completion ledger. */
public final class WildlifeQuestSetups extends SavedData {
    private static final int LIMIT = 8192;
    private final Map<String, CompoundTag> setups = new LinkedHashMap<>();
    public static WildlifeQuestSetups get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(WildlifeQuestSetups::load,
                WildlifeQuestSetups::new, "jem_wildlife_setups_v4");
    }
    private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "/" + pos.asLong(); }
    public CompoundTag at(ServerLevel level, BlockPos pos) { return setups.getOrDefault(key(level, pos), new CompoundTag()).copy(); }
    public void put(ServerLevel level, BlockPos pos, CompoundTag proof) {
        String key = key(level, pos);
        if (proof.isEmpty()) setups.remove(key);
        else {
            if (!setups.containsKey(key) && setups.size() >= LIMIT) setups.remove(setups.keySet().iterator().next());
            setups.put(key, proof.copy());
        }
        setDirty();
    }
    public static String block(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    public static int property(BlockState state, String name) {
        for (var property : state.getProperties()) if (property.getName().equals(name) && state.getValue(property) instanceof Integer value) return value;
        return -1;
    }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (!level.getServer().isSameThread()) return;
        var data = get(level); var proof = data.at(level, pos); if (proof.isEmpty()) return;
        if (before.getBlock() != after.getBlock() || block(after).equals("alexsmobs:hummingbird_feeder") && property(after, "contents") == 0) data.put(level, pos, new CompoundTag());
    }
    private static WildlifeQuestSetups load(CompoundTag tag) {
        var data = new WildlifeQuestSetups(); ListTag rows = tag.getList("setups", 10);
        for (int i = 0; i < Math.min(LIMIT, rows.size()); i++) { var row = rows.getCompound(i); data.setups.put(row.getString("key"), row.getCompound("proof").copy()); }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag(); setups.forEach((key, proof) -> { CompoundTag row = new CompoundTag(); row.putString("key", key); row.put("proof", proof.copy()); rows.add(row); });
        tag.put("setups", rows); return tag;
    }
}
