package com.siirio.jemsettlements.resident;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Persistent mood belongs only to a sanctioned vanilla resident UUID. */
public final class ResidentMood {
    private final UUID villagerId;
    private final UUID townId;
    private GlobalPos home;
    private GlobalPos workplace;
    private int mood = 50;
    private int satiety = 50;
    private long lastServingGameTime = Long.MIN_VALUE;
    private long lastDailyUpdate = Long.MIN_VALUE;
    private final Deque<String> recentGroups = new ArrayDeque<>();
    private final Map<String, Long> foodCooldownDays = new LinkedHashMap<>();
    private final Map<Long, Integer> positiveByDay = new LinkedHashMap<>();
    private final Map<Long, Integer> negativeByDay = new LinkedHashMap<>();
    private AmenitySnapshot homeAmenities = AmenitySnapshot.empty();
    private AmenitySnapshot workAmenities = AmenitySnapshot.empty();
    private long amenityDirtySince;
    private boolean living = true;

    public ResidentMood(UUID villagerId, UUID townId, GlobalPos home, GlobalPos workplace) {
        this.villagerId = java.util.Objects.requireNonNull(villagerId); this.townId = java.util.Objects.requireNonNull(townId); this.home = home; this.workplace = workplace;
    }
    public UUID villagerId() { return villagerId; }
    public UUID townId() { return townId; }
    public Optional<GlobalPos> home() { return Optional.ofNullable(home); }
    public Optional<GlobalPos> workplace() { return Optional.ofNullable(workplace); }
    public int mood() { return mood; }
    public int satiety() { return satiety; }
    public long lastServingGameTime() { return lastServingGameTime; }
    public AmenitySnapshot homeAmenities() { return homeAmenities; }
    public AmenitySnapshot workAmenities() { return workAmenities; }
    public long amenityDirtySince() { return amenityDirtySince; }
    public boolean living() { return living; }
    public void markDead() { living = false; }
    public void seen(GlobalPos currentHome, GlobalPos currentWorkplace) { living = true; if (currentHome != null) home = currentHome; if (currentWorkplace != null) workplace = currentWorkplace; }
    public void markAmenitiesDirty(long gameTime) { if (amenityDirtySince == 0L) amenityDirtySince = Math.max(1L, gameTime); }
    public void homeAmenities(AmenitySnapshot value) { homeAmenities = value; amenityDirtySince = 0L; }
    public void workAmenities(AmenitySnapshot value) { workAmenities = value; amenityDirtySince = 0L; }
    public boolean canEat(long gameTime, ResourceLocation food) { return living && satiety < 60 && (lastServingGameTime == Long.MIN_VALUE || gameTime - lastServingGameTime >= 12_000L) && foodCooldownDays.getOrDefault(food.toString(), Long.MIN_VALUE) < Math.floorDiv(gameTime, 24_000L); }
    public int acceptFood(ResourceLocation food, String group, int baseMood, int nutrition, long gameTime) {
        long day = Math.floorDiv(gameTime, 24_000L); pruneStimuli(day); boolean repeatedGroup = recentGroups.contains(group); int applied = baseMood > 0 && repeatedGroup ? baseMood / 4 : baseMood;
        if (applied > 0) { int used = positiveByDay.values().stream().mapToInt(Integer::intValue).sum(); applied = Math.min(applied, Math.max(0, 15 - used)); positiveByDay.merge(day, applied, Integer::sum); }
        else if (applied < 0) { int used = -negativeByDay.values().stream().mapToInt(Integer::intValue).sum(); applied = Math.max(applied, -Math.max(0, 20 - used)); negativeByDay.merge(day, applied, Integer::sum); }
        mood = clamp(mood + applied); satiety = clamp(satiety + Math.max(0, nutrition) * 4); lastServingGameTime = gameTime; foodCooldownDays.put(food.toString(), day);
        recentGroups.remove(group); recentGroups.addFirst(group); while (recentGroups.size() > 4) recentGroups.removeLast(); return applied;
    }
    public void dailyUpdate(long day) {
        if (lastDailyUpdate == Long.MIN_VALUE) { lastDailyUpdate = day; return; }
        long elapsed = Math.min(6L, Math.max(0L, day - lastDailyUpdate));
        for (int i = 0; i < elapsed; i++) { satiety = Math.max(0, satiety - 12); if (mood < 50) mood = Math.min(50, mood + 2); else if (mood > 50) mood = Math.max(50, mood - 2); }
        lastDailyUpdate = Math.max(lastDailyUpdate, day); pruneStimuli(day);
    }
    private void pruneStimuli(long day) { positiveByDay.keySet().removeIf(value -> value < day - 6L); negativeByDay.keySet().removeIf(value -> value < day - 6L); foodCooldownDays.entrySet().removeIf(value -> value.getValue() < day - 2L); }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("Villager", villagerId); tag.putUUID("Town", townId); savePos(tag, "Home", home); savePos(tag, "Work", workplace);
        tag.putInt("Mood", mood); tag.putInt("Satiety", satiety); tag.putLong("LastServing", lastServingGameTime); tag.putLong("LastDailyUpdate", lastDailyUpdate); tag.putBoolean("Living", living);
        tag.put("HomeAmenities", homeAmenities.save()); tag.put("WorkAmenities", workAmenities.save()); tag.putLong("AmenityDirtySince", amenityDirtySince);
        ListTag groups = new ListTag(); recentGroups.forEach(value -> { CompoundTag row = new CompoundTag(); row.putString("Group", value); groups.add(row); }); tag.put("RecentGroups", groups);
        CompoundTag foods = new CompoundTag(); foodCooldownDays.forEach(foods::putLong); tag.put("FoodDays", foods); tag.put("PositiveDays", saveLongIntMap(positiveByDay)); tag.put("NegativeDays", saveLongIntMap(negativeByDay)); return tag;
    }
    public static ResidentMood load(CompoundTag tag) {
        ResidentMood resident = new ResidentMood(tag.getUUID("Villager"), tag.getUUID("Town"), loadPos(tag, "Home"), loadPos(tag, "Work"));
        resident.mood = tag.contains("Mood") ? clamp(tag.getInt("Mood")) : 50; resident.satiety = tag.contains("Satiety") ? clamp(tag.getInt("Satiety")) : 50;
        resident.lastServingGameTime = tag.contains("LastServing") ? tag.getLong("LastServing") : Long.MIN_VALUE; resident.lastDailyUpdate = tag.contains("LastDailyUpdate") ? tag.getLong("LastDailyUpdate") : Long.MIN_VALUE;
        resident.living = !tag.contains("Living") || tag.getBoolean("Living"); if (tag.contains("HomeAmenities")) resident.homeAmenities = AmenitySnapshot.load(tag.getCompound("HomeAmenities"));
        if (tag.contains("WorkAmenities")) resident.workAmenities = AmenitySnapshot.load(tag.getCompound("WorkAmenities")); resident.amenityDirtySince = tag.getLong("AmenityDirtySince");
        ListTag groups = tag.getList("RecentGroups", Tag.TAG_COMPOUND); for (int i = 0; i < groups.size() && i < 4; i++) resident.recentGroups.addLast(groups.getCompound(i).getString("Group"));
        CompoundTag foods = tag.getCompound("FoodDays"); foods.getAllKeys().forEach(key -> resident.foodCooldownDays.put(key, foods.getLong(key)));
        loadLongIntMap(tag.getList("PositiveDays", Tag.TAG_COMPOUND), resident.positiveByDay); loadLongIntMap(tag.getList("NegativeDays", Tag.TAG_COMPOUND), resident.negativeByDay); return resident;
    }
    private static void savePos(CompoundTag tag, String key, GlobalPos pos) { if (pos != null) { CompoundTag row = new CompoundTag(); row.putString("Dimension", pos.dimension().location().toString()); row.putLong("Position", pos.pos().asLong()); tag.put(key, row); } }
    private static GlobalPos loadPos(CompoundTag tag, String key) { if (!tag.contains(key, Tag.TAG_COMPOUND)) return null; CompoundTag row = tag.getCompound(key); ResourceLocation id = ResourceLocation.tryParse(row.getString("Dimension")); return id == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), BlockPos.of(row.getLong("Position"))); }
    private static ListTag saveLongIntMap(Map<Long, Integer> values) { ListTag rows = new ListTag(); values.forEach((day, value) -> { CompoundTag row = new CompoundTag(); row.putLong("Day", day); row.putInt("Value", value); rows.add(row); }); return rows; }
    private static void loadLongIntMap(ListTag rows, Map<Long, Integer> target) { for (int i = 0; i < rows.size(); i++) { CompoundTag row = rows.getCompound(i); target.put(row.getLong("Day"), row.getInt("Value")); } }
}
