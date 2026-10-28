package com.justenoughmods.achievementguide.integration.spawncare;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
public final class SpawnCareQuestSetups extends SavedData {
    private static final int LIMIT = 8192;
    private static final Map<net.minecraft.server.MinecraftServer, SpawnCareQuestSetups> LOADED = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<String, CompoundTag> records = new LinkedHashMap<>();
    public static SpawnCareQuestSetups get(ServerLevel level) { var data = level.getServer().overworld().getDataStorage().computeIfAbsent(SpawnCareQuestSetups::load, SpawnCareQuestSetups::new, "jem_spawn_care_setups_v4"); LOADED.put(level.getServer(), data); return data; }
    public static String block(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "/" + pos.asLong(); }
    public CompoundTag at(ServerLevel level, BlockPos pos) { return records.getOrDefault(key(level, pos), new CompoundTag()).copy(); }
    public void put(ServerLevel level, BlockPos pos, CompoundTag proof) { String key = key(level, pos); if (proof.isEmpty()) { if (records.remove(key) != null) setDirty(); } else if (records.size() < LIMIT || records.containsKey(key)) { records.put(key, proof.copy()); setDirty(); } }
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) { if (before.getBlock() != after.getBlock()) { var data = LOADED.get(level.getServer()); if (data == null) data = get(level); if (data.records.containsKey(key(level, pos))) data.put(level, pos, new CompoundTag()); } }
    private static SpawnCareQuestSetups load(CompoundTag tag) { var data = new SpawnCareQuestSetups(); var rows = tag.getList("setups", 10); for (int i = 0; i < Math.min(rows.size(), LIMIT); i++) { var row = rows.getCompound(i); data.records.put(row.getString("key"), row.getCompound("proof").copy()); } return data; }
    @Override public CompoundTag save(CompoundTag tag) { ListTag rows = new ListTag(); records.forEach((key, proof) -> { var row = new CompoundTag(); row.putString("key", key); row.put("proof", proof.copy()); rows.add(row); }); tag.put("setups", rows); return tag; }
}
