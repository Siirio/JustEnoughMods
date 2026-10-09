package com.siirio.jemserver.smp.events;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.siirio.jemserver.smp.SmpRecords;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import com.siirio.jemworldbosstiers.encounter.RewardGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BloodMoonRewards extends SimpleJsonResourceReloadListener {
    private static final String PENDING = "pendingWaveRewards";
    private static final double CONSOLATION_SHARE = .15;
    private static final double CONSOLATION_TYPE_SHARE = .25;
    private static Map<Integer, Wave> waves = Map.of();

    private BloodMoonRewards() {
        super(new Gson(), "jem/events/blood_moon_rewards");
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        event.addListener(new BloodMoonRewards());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        Map<Integer, Wave> parsed = new LinkedHashMap<>();
        for (JsonElement value : data.values()) {
            for (JsonElement element : value.getAsJsonObject().getAsJsonArray("waves")) {
                JsonObject row = element.getAsJsonObject();
                int number = row.get("wave").getAsInt();
                if (number < 1 || number > BloodMoonWaves.WAVES || parsed.containsKey(number))
                    throw new IllegalArgumentException("Invalid or duplicate Blood Moon wave " + number);
                List<Reward> guaranteed = rewards(row.getAsJsonArray("guaranteed"));
                List<Reward> random = rewards(row.getAsJsonArray("random"));
                int randomCount = row.get("random_count").getAsInt();
                if (guaranteed.isEmpty() || randomCount < 0 || randomCount > random.size())
                    throw new IllegalArgumentException("Invalid Blood Moon reward definition for wave " + number);
                parsed.put(number, new Wave(number, row.get("title").getAsString(), guaranteed, random, randomCount));
            }
        }
        if (parsed.size() != BloodMoonWaves.WAVES)
            throw new IllegalArgumentException("Blood Moon rewards require exactly five waves");
        waves = Map.copyOf(parsed);
    }

    private static List<Reward> rewards(Iterable<JsonElement> values) {
        List<Reward> result = new ArrayList<>();
        for (JsonElement element : values) {
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString());
            int count = row.get("count").getAsInt();
            int enchantPower = row.has("enchant_power") ? row.get("enchant_power").getAsInt() : 0;
            if (id == null || count < 1 || count > 64 || enchantPower < 0 || enchantPower > 50)
                throw new IllegalArgumentException("Invalid Blood Moon reward entry");
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == Items.AIR) {
                org.slf4j.LoggerFactory.getLogger(BloodMoonRewards.class).warn("Skipping unavailable Blood Moon reward {}", id);
                continue;
            }
            result.add(new Reward(item, count, enchantPower));
        }
        return List.copyOf(result);
    }

    public static void stage(ServerLevel level, CompoundTag event, List<net.minecraft.server.level.ServerPlayer> participants, int wave) {
        Wave definition = requireWave(wave);
        CompoundTag pending = event.getCompound(PENDING);
        for (var player : participants) {
            CompoundTag member = SmpRecords.members(event).getCompound(player.getStringUUID());
            if (!member.getBoolean("contributed")) continue;
            ListTag caches = pending.getList(player.getStringUUID(), Tag.TAG_COMPOUND);
            if (caches.stream().map(tag -> (CompoundTag) tag).anyMatch(cache -> cache.getInt("wave") == wave)) continue;
            CompoundTag cache = new CompoundTag();
            cache.putInt("wave", wave);
            cache.putString("titleKey", definition.titleKey());
            cache.put("items", save(definition.roll(level, BloodMoonWaves.factor(wave))));
            caches.add(cache);
            pending.put(player.getStringUUID(), caches);
        }
        event.put(PENDING, pending);
    }

    public static CompoundTag previewWave(int wave) {
        Wave definition = requireWave(wave);
        CompoundTag preview = new CompoundTag();
        preview.putInt("wave", wave);
        preview.putString("titleKey", definition.titleKey());
        preview.putInt("randomCount", definition.randomCount());
        preview.put("guaranteed", preview(definition.guaranteed(), BloodMoonWaves.factor(wave)));
        preview.put("random", preview(definition.random(), BloodMoonWaves.factor(wave)));
        return preview;
    }

    public static ListTag preview() {
        ListTag result = new ListTag();
        waves.values().stream().sorted(Comparator.comparingInt(Wave::number))
                .forEach(wave -> result.addAll(preview(wave.guaranteed(), BloodMoonWaves.factor(wave.number()))));
        return result;
    }

    public static void commit(MinecraftServer server, CompoundTag event, boolean fullCompletion) {
        CompoundTag pending = event.getCompound(PENDING);
        for (String playerId : pending.getAllKeys()) {
            List<RewardGroup> groups = groups(pending.getList(playerId, Tag.TAG_COMPOUND));
            if (!groups.isEmpty()) PendingRewardContainer.enqueueGrouped(server, UUID.fromString(playerId), event.getUUID("id").toString(),
                    fullCompletion ? "jem.smp.reward.blood_moon.full" : "jem.smp.reward.blood_moon.cash_out", groups);
        }
        event.remove(PENDING);
    }

    public static void commitConsolation(MinecraftServer server, CompoundTag event) {
        CompoundTag pending = event.getCompound(PENDING);
        for (String playerId : pending.getAllKeys()) {
            List<ItemStack> accumulated = new ArrayList<>();
            for (Tag value : pending.getList(playerId, Tag.TAG_COMPOUND))
                ((CompoundTag) value).getList("items", Tag.TAG_COMPOUND).forEach(item -> accumulated.add(ItemStack.of((CompoundTag) item)));
            UUID player = UUID.fromString(playerId);
            List<ItemStack> consolation = consolation(accumulated, event.getUUID("id"), player);
            if (!consolation.isEmpty()) PendingRewardContainer.enqueueGrouped(server, player, event.getUUID("id") + ":consolation",
                    "jem.smp.reward.blood_moon.failure", List.of(new RewardGroup("jem.smp.reward.blood_moon.consolation", consolation)));
        }
        event.remove(PENDING);
    }

    private static List<ItemStack> consolation(List<ItemStack> source, UUID event, UUID player) {
        List<ItemStack> shuffled = new ArrayList<>(source.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList());
        if (shuffled.isEmpty()) return List.of();
        Collections.shuffle(shuffled, new java.util.Random(event.getMostSignificantBits() ^ player.getLeastSignificantBits()));
        int typeLimit = Math.max(1, (int) Math.ceil(shuffled.size() * CONSOLATION_TYPE_SHARE));
        long target = Math.max(1, Math.round(shuffled.stream().mapToLong(stack -> value(stack) * stack.getCount()).sum() * CONSOLATION_SHARE));
        List<ItemStack> result = new ArrayList<>();
        long awarded = 0;
        for (ItemStack stack : shuffled) {
            if (result.size() >= typeLimit || awarded >= target) break;
            long unit = value(stack);
            int count = Math.max(1, Math.min(stack.getCount(), (int) Math.max(1, (target - awarded) / unit)));
            result.add(stack.copyWithCount(count));
            awarded += unit * count;
        }
        return result;
    }

    private static long value(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.NETHERITE_SCRAP || item == Items.ANCIENT_DEBRIS) return 24;
        if (item == Items.TOTEM_OF_UNDYING || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.ENCHANTED_BOOK) return 32;
        if (item == Items.DIAMOND_BLOCK) return 18;
        if (item == Items.EMERALD_BLOCK || item == Items.GOLD_BLOCK) return 10;
        if (item == Items.DIAMOND || item == Items.EMERALD) return 3;
        return 1;
    }

    private static List<RewardGroup> groups(ListTag caches) {
        List<RewardGroup> groups = new ArrayList<>();
        caches.stream().map(tag -> (CompoundTag) tag).sorted(Comparator.comparingInt(cache -> cache.getInt("wave")))
                .forEach(cache -> groups.add(new RewardGroup(cache.getString("titleKey"), load(cache.getList("items", Tag.TAG_COMPOUND)))));
        return groups;
    }

    private static ListTag preview(List<Reward> rewards, double factor) {
        ListTag result = new ListTag();
        rewards.forEach(reward -> result.add(reward.preview(factor)));
        return result;
    }

    private static ListTag save(List<ItemStack> items) {
        ListTag result = new ListTag();
        items.forEach(stack -> result.add(stack.save(new CompoundTag())));
        return result;
    }

    private static List<ItemStack> load(ListTag items) {
        List<ItemStack> result = new ArrayList<>();
        items.forEach(item -> result.add(ItemStack.of((CompoundTag) item)));
        return result;
    }

    private static Wave requireWave(int wave) {
        Wave definition = waves.get(wave);
        if (definition == null) throw new IllegalStateException("Blood Moon reward data is unavailable");
        return definition;
    }

    private record Reward(Item item, int count, int enchantPower) {
        ItemStack roll(ServerLevel level, double factor) {
            return enchantPower > 0 ? EnchantmentHelper.enchantItem(level.random, new ItemStack(Items.BOOK), enchantPower, false) : new ItemStack(item, scaledCount(factor));
        }
        CompoundTag preview(double factor) {
            CompoundTag result = new ItemStack(item, scaledCount(factor)).save(new CompoundTag());
            if (enchantPower > 0) result.putInt("previewEnchantPower", enchantPower);
            return result;
        }
        private int scaledCount(double factor) { return enchantPower > 0 ? count : Math.max(1, (int)Math.ceil(count * factor)); }
    }

    private record Wave(int number, String titleKey, List<Reward> guaranteed, List<Reward> random, int randomCount) {
        List<ItemStack> roll(ServerLevel level, double factor) {
            List<ItemStack> result = new ArrayList<>();
            guaranteed.forEach(reward -> result.add(reward.roll(level, factor)));
            List<Reward> candidates = new ArrayList<>(random);
            for (int index = 0; index < randomCount && !candidates.isEmpty(); index++)
                result.add(candidates.remove(level.random.nextInt(candidates.size())).roll(level, factor));
            return result;
        }
    }
}
