package com.siirio.jemworldbosstiers.encounter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class RaidRewardPool extends SimpleJsonResourceReloadListener {
    private static List<Resource> resources = List.of();
    private static List<Integer> bookLevels = List.of();

    private RaidRewardPool() { super(new Gson(), "jem_world_boss_tiers/raid_rewards"); }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) { event.addListener(new RaidRewardPool()); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        List<Resource> parsed = new ArrayList<>();
        List<Integer> levels = List.of();
        for (JsonElement value : data.values()) {
            var root = value.getAsJsonObject();
            if (root.has("book_levels")) {
                if (!levels.isEmpty()) throw new IllegalArgumentException("Only one raid reward book level definition is allowed");
                levels = root.getAsJsonArray("book_levels").asList().stream().map(JsonElement::getAsInt).toList();
            }
            for (JsonElement element : root.getAsJsonArray("resources")) {
                var entry = element.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(entry.get("item").getAsString());
                int weight = entry.get("weight").getAsInt();
                List<Integer> counts = entry.getAsJsonArray("counts").asList().stream().map(JsonElement::getAsInt).toList();
                if (weight <= 0 || counts.size() != 5 || counts.stream().anyMatch(count -> count <= 0 || count > 64))
                    throw new IllegalArgumentException("Invalid raid reward: " + id);
                if (parsed.stream().anyMatch(resource -> resource.id().equals(id))) throw new IllegalArgumentException("Duplicate raid reward: " + id);
                if (!BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id) == Items.AIR) {
                    LogUtils.getLogger().warn("Skipping unavailable raid reward material {}", id);
                    continue;
                }
                parsed.add(new Resource(id, weight, counts));
            }
        }
        if (!parsed.isEmpty() && (parsed.size() < 2 || levels.size() != 5 || levels.stream().anyMatch(level -> level < 1 || level > 50)))
            throw new IllegalArgumentException("Raid rewards require two materials and five valid book levels");
        resources = List.copyOf(parsed);
        bookLevels = levels;
    }

    public static List<ItemStack> roll(ServerLevel level, int tier) {
        if (resources.size() < 2) {
            LogUtils.getLogger().error("Raid reward data is missing; no personal bonus could be rolled");
            return List.of();
        }
        int index = Math.max(0, Math.min(bookLevels.size() - 1, tier - 1));
        List<Resource> candidates = new ArrayList<>(resources);
        List<ItemStack> result = new ArrayList<>();
        for (int roll = 0; roll < 2; roll++) {
            int totalWeight = candidates.stream().mapToInt(Resource::weight).sum();
            int selected = weightedIndex(candidates.stream().map(Resource::weight).toList(), level.random.nextInt(totalWeight));
            Resource resource = candidates.remove(selected);
            result.add(new ItemStack(BuiltInRegistries.ITEM.get(resource.id()), resource.counts().get(index)));
        }
        result.add(EnchantmentHelper.enchantItem(level.random, new ItemStack(Items.BOOK), bookLevels.get(index), true));
        return result;
    }

    public static List<ItemStack> preview(int tier) {
        int index = Math.max(0, Math.min(4, tier - 1));
        List<ItemStack> items = new ArrayList<>();
        for (Resource resource : resources) items.add(new ItemStack(BuiltInRegistries.ITEM.get(resource.id()), resource.counts().get(index)));
        items.add(new ItemStack(Items.ENCHANTED_BOOK));
        return items;
    }

    static int weightedIndex(List<Integer> weights, int roll) {
        for (int index = 0; index < weights.size(); index++) {
            roll -= weights.get(index);
            if (roll < 0) return index;
        }
        throw new IllegalArgumentException("Roll exceeds reward weight");
    }

    private record Resource(ResourceLocation id, int weight, List<Integer> counts) {}
}
