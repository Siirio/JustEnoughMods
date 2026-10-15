package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record FoodPolicyPacket(CompoundTag payload) {
    static FoodPolicyPacket snapshot() {
        CompoundTag root = new CompoundTag(); ListTag rows = new ListTag();
        FoodValueRegistry.snapshot().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag row = new CompoundTag(); row.putString("Item", entry.getKey().toString()); row.putInt("Mood", entry.getValue().mood()); row.putInt("Satiety", entry.getValue().satiety());
            row.putString("Group", entry.getValue().group()); row.putString("Classification", entry.getValue().classification()); rows.add(row);
        }); root.put("Rows", rows); return new FoodPolicyPacket(root);
    }
    public Map<ResourceLocation, FoodValueRegistry.Value> values() {
        Map<ResourceLocation, FoodValueRegistry.Value> result = new LinkedHashMap<>(); ListTag rows = payload.getList("Rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) { CompoundTag row = rows.getCompound(i); ResourceLocation id = ResourceLocation.tryParse(row.getString("Item")); if (id != null) result.put(id, new FoodValueRegistry.Value(row.getInt("Mood"), row.getInt("Satiety"), row.getString("Group"), row.getString("Classification"))); }
        return result;
    }
    static void encode(FoodPolicyPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.payload); }
    static FoodPolicyPacket decode(FriendlyByteBuf buffer) { CompoundTag tag = buffer.readNbt(); return new FoodPolicyPacket(tag == null ? new CompoundTag() : tag); }
}
