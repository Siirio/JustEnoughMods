package com.siirio.jemsettlements.happiness;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
/** Actual player dimension visits, persisted independently of optional SMP. */
public final class DishProgression extends SavedData {
    private final Set<ResourceLocation> visited = new HashSet<>();
    public static DishProgression get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(DishProgression::load, DishProgression::new, "jem_settlement_dish_progress"); }
    public void visit(ResourceLocation dimension) { if (visited.add(dimension)) setDirty(); }
    public boolean visited(String dimension) { return visited.contains(new ResourceLocation(dimension)); }
    @Override public CompoundTag save(CompoundTag tag) { ListTag rows = new ListTag(); visited.stream().sorted().forEach(id -> rows.add(StringTag.valueOf(id.toString()))); tag.put("Visited", rows); return tag; }
    private static DishProgression load(CompoundTag tag) { DishProgression data = new DishProgression(); for (Tag row : tag.getList("Visited", Tag.TAG_STRING)) { ResourceLocation id = ResourceLocation.tryParse(row.getAsString()); if (id != null) data.visited.add(id); } return data; }
}
