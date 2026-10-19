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

/** Resident needs, or serving metadata bridged to a native recruit needs owner. */
public final class ResidentMood {
    private final UUID villagerId;
    private final UUID townId;
    private GlobalPos home;
    private GlobalPos workplace;
    private CompoundTag needObservation = new CompoundTag();
    public CompoundTag needObservation() { return needObservation.copy(); }
    public void needObservation(CompoundTag value) { needObservation = value.copy(); }
    private int mood = 50;
    private int environmentBonus;
    private int socialMoodCap = 100;
    private GlobalPos crowdedHome;
    public Optional<GlobalPos> crowdedHome() { return Optional.ofNullable(crowdedHome); }
    public void crowdedHome(GlobalPos value) { crowdedHome = value; }
    private GlobalPos observedBed;
    public Optional<GlobalPos> observedBed() { return Optional.ofNullable(observedBed); }
    public void observedBed(GlobalPos value) { observedBed = value; }
    private int crowdingPenalty, lightPenalty, fearPenalty;
    private boolean conditionsUnknown;
    public int crowdingPenalty() { return crowdingPenalty; }
    public int lightPenalty() { return lightPenalty; }
    public int fearPenalty() { return fearPenalty; }
    public int conditionsPenalty() { return crowdingPenalty + lightPenalty + fearPenalty; }
    public boolean conditionsUnknown() { return conditionsUnknown; }
    public void crowdingPenalty(int value) { crowdingPenalty = clamp(value); }
    public void lightPenalty(int value) { lightPenalty = clamp(value); }
    public void fearPenalty(int value) { fearPenalty = clamp(value); }
    public void conditionsUnknown(boolean value) { conditionsUnknown = value; }
    private int satiety = 50;
    private long lastServingGameTime = Long.MIN_VALUE;
    private long lastDailyUpdate = Long.MIN_VALUE;
    private final Deque<String> recentFoods = new ArrayDeque<>();
    private final Map<String, Long> foodCooldownDays = new LinkedHashMap<>();
    private final Map<String, Integer> servingsToday = new LinkedHashMap<>();
    private final Map<Long, Integer> positiveByDay = new LinkedHashMap<>();
    private final Map<Long, Integer> negativeByDay = new LinkedHashMap<>();
    private AmenitySnapshot homeAmenities = AmenitySnapshot.empty();
    private AmenitySnapshot workAmenities = AmenitySnapshot.empty();
    private long amenityDirtySince;
    private boolean living = true;
    private boolean child;
    private long lastPatDay = Long.MIN_VALUE;
    public boolean child() { return child; }
    public boolean pat(long day) { if (!living || day <= lastPatDay) return false; lastPatDay = day; mood = clamp(mood + 8); return true; }
    private String displayName;
    private String profession = "minecraft:none";
    public String displayName() { return displayName; }
    public String profession() { return profession; }
    public void observeName(net.minecraft.world.entity.npc.Villager villager) {
        child = villager.isBaby();
        displayName = net.minecraft.network.chat.Component.Serializer.toJson(villager.getCustomName()!=null?villager.getCustomName():villager.getName());
        profession = net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString();
    }
    private ResourceLocation requestedDish;
    private ResourceLocation lastDish;
    private boolean repeatedDish;
    private boolean craving;
    private int cravingThreshold;
    private int cravingCycle;
    private long servingDay = Long.MIN_VALUE;
    private int regenerationTicks;

    public ResidentMood(UUID villagerId, UUID townId, GlobalPos home, GlobalPos workplace) {
        this.villagerId = java.util.Objects.requireNonNull(villagerId); this.townId = java.util.Objects.requireNonNull(townId); this.home = home; this.workplace = workplace;
        this.cravingThreshold = nextCravingThreshold();
    }
    public UUID villagerId() { return villagerId; }
    public UUID townId() { return townId; }
    public Optional<GlobalPos> home() { return Optional.ofNullable(home); }
    public Optional<GlobalPos> workplace() { return Optional.ofNullable(workplace); }
    public int mood() { return clamp(com.siirio.jemsettlements.happiness.HappinessService.residentMood(mood, environmentBonus, socialMoodCap) - conditionsPenalty()); }
    public int personalMood() { return mood; }
    public int environmentBonus() { return environmentBonus; }
    public int socialMoodCap() { return socialMoodCap; }
    public void environment(int comfort, int social, int animals) {
        environmentBonus = com.siirio.jemsettlements.happiness.HappinessService.environmentBonus(comfort, animals);
        socialMoodCap = com.siirio.jemsettlements.happiness.HappinessService.socialCap(social);
    }
    public int satiety() { return satiety; }
    public int hunger() { return 100 - satiety; }
    /** Native Recruits owns these values; this snapshot only drives shared serving rules. */
    public void nativeNeeds(float morale, float hunger) {
        mood = clamp(Math.round(morale)); satiety = clamp((int) Math.floor(hunger));
        if (satiety >= 100 && craving) { craving = false; cravingCycle++; cravingThreshold = nextCravingThreshold(); requestedDish = null; }
    }
    public boolean craving() { return craving; }
    public int cravingThreshold() { return cravingThreshold; }
    public Optional<ResourceLocation> requestedDish() { return Optional.ofNullable(requestedDish); }
    public Optional<ResourceLocation> repeatedDish() { return repeatedDish ? Optional.ofNullable(lastDish) : Optional.empty(); }
    public boolean canRequest(ResourceLocation dish) { return dish != null && !recentFoods.contains(dish.toString()); }
    public long lastServingGameTime() { return lastServingGameTime; }
    /** Loaded time only: one half-heart per four seconds while well fed, at a food cost. */
    public boolean regenerate(boolean canHeal) {
        if (!canHeal || !living || satiety < 90 || lastServingGameTime == Long.MIN_VALUE) {
            regenerationTicks = 0;
            return false;
        }
        regenerationTicks += 20;
        if (regenerationTicks < 80) return false;
        regenerationTicks = 0;
        satiety = Math.max(0, satiety - 4);
        return true;
    }
    public AmenitySnapshot homeAmenities() { return homeAmenities; }
    public AmenitySnapshot workAmenities() { return workAmenities; }
    public long amenityDirtySince() { return amenityDirtySince; }
    public boolean living() { return living; }
    public void markDead() { living = false; }
    public void seen(GlobalPos currentHome, GlobalPos currentWorkplace) { living = true; if (!java.util.Objects.equals(home, currentHome) || !java.util.Objects.equals(workplace, currentWorkplace)) markAmenitiesDirty(1L); home = currentHome; workplace = currentWorkplace; }
    public void markAmenitiesDirty(long gameTime) { if (amenityDirtySince == 0L) amenityDirtySince = Math.max(1L, gameTime); }
    public void homeAmenities(AmenitySnapshot value) { homeAmenities = value; amenityDirtySince = 0L; }
    public void workAmenities(AmenitySnapshot value) { workAmenities = value; amenityDirtySince = 0L; }
    public boolean canEat(long gameTime, ResourceLocation food) {
        long day = Math.floorDiv(gameTime, 24_000L);
        resetServingDay(day);
        return living && craving && satiety < 100 && servingsToday.getOrDefault(food.toString(), 0) < 10
                && (lastServingGameTime == Long.MIN_VALUE || gameTime - lastServingGameTime >= 32L);
    }
    public void requestMeal() { if (living && satiety < 100) craving = true; }
    public void refreshCraving() { if (!craving && satiety < 100 && (satiety < 10 || satiety <= cravingThreshold)) craving = true; }
    public void abandonCraving() {
        if (satiety < 10) return;
        craving = false;
        cravingCycle++;
        cravingThreshold = Math.max(10, Math.min(satiety - 1, nextCravingThreshold()));
    }
    public void requestedDish(ResourceLocation value) { requestedDish = value; }
    public void retainRequestedDishes(java.util.Collection<ResourceLocation> available) {
        if (requestedDish != null && !available.contains(requestedDish)) requestedDish = null;
    }
    public int acceptFood(ResourceLocation food, int baseMood, int satietyDelta, long gameTime) {
        long day = Math.floorDiv(gameTime, 24_000L); resetServingDay(day); pruneStimuli(day);
        boolean repeated = recentFoods.contains(food.toString());
        int applied = baseMood < 0 ? baseMood : repeated ? 1 : Math.max(1, baseMood);
        if (food.equals(requestedDish) && baseMood >= 0) { applied += 4; requestedDish = null; }
        if (applied > 0) { int used = positiveByDay.values().stream().mapToInt(Integer::intValue).sum(); applied = Math.min(applied, Math.max(0, 15 - used)); positiveByDay.merge(day, applied, Integer::sum); }
        else if (applied < 0) { int used = -negativeByDay.values().stream().mapToInt(Integer::intValue).sum(); applied = Math.max(applied, -Math.max(0, 20 - used)); negativeByDay.merge(day, applied, Integer::sum); }
        mood = clamp(mood + applied); satiety = clamp(satiety + satietyDelta); lastServingGameTime = gameTime; foodCooldownDays.put(food.toString(), day);
        servingsToday.merge(food.toString(), 1, Integer::sum);
        if (satiety >= 100) { craving = false; cravingCycle++; cravingThreshold = nextCravingThreshold(); requestedDish = null; }
        repeatedDish = repeated; lastDish = food;
        recentFoods.remove(food.toString()); recentFoods.addFirst(food.toString()); while (recentFoods.size() > 4) recentFoods.removeLast(); return applied;
    }
    public int damaged(float amount) { int penalty = Math.max(1, Math.min(10, (int) Math.ceil(amount))); mood = clamp(mood - penalty); return penalty; }
    public void dailyUpdate(long day) {
        if (lastDailyUpdate == Long.MIN_VALUE) { lastDailyUpdate = day; return; }
        long elapsed = Math.min(6L, Math.max(0L, day - lastDailyUpdate));
        for (int i = 0; i < elapsed; i++) { satiety = Math.max(0, satiety - 12); if (mood < 50) mood = Math.min(50, mood + 2); else if (mood > 50) mood = Math.max(50, mood - 2); }
        lastDailyUpdate = Math.max(lastDailyUpdate, day); pruneStimuli(day);
    }
    private void resetServingDay(long day) {
        if (servingDay == Long.MIN_VALUE || day > servingDay) {
            servingDay = day;
            servingsToday.clear();
        }
    }
    private void pruneStimuli(long day) { positiveByDay.keySet().removeIf(value -> value < day - 6L); negativeByDay.keySet().removeIf(value -> value < day - 6L); foodCooldownDays.entrySet().removeIf(value -> value.getValue() < day - 2L); }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private int nextCravingThreshold() {
        long mixed = villagerId.getMostSignificantBits() ^ Long.rotateLeft(villagerId.getLeastSignificantBits(), 17) ^ (long) cravingCycle * 0x9E3779B97F4A7C15L;
        int roll = Math.floorMod(Long.hashCode(mixed), 100);
        if (roll < 60) return 70 + Math.floorMod(Long.hashCode(mixed >>> 7), 30);
        return 35 + Math.floorMod(Long.hashCode(mixed >>> 13), 35);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("Villager", villagerId); tag.putUUID("Town", townId); savePos(tag, "Home", home); savePos(tag, "Work", workplace);
        if (displayName != null) tag.putString("DisplayName", displayName);
        tag.putString("Profession", profession); tag.putBoolean("Child", child); tag.putLong("LastPatDay", lastPatDay);
        tag.put("NeedObservation", needObservation.copy()); tag.putInt("Mood", mood); tag.putInt("EnvironmentBonus", environmentBonus); tag.putInt("SocialMoodCap", socialMoodCap); tag.putInt("Satiety", satiety); tag.putLong("LastServing", lastServingGameTime); tag.putLong("LastDailyUpdate", lastDailyUpdate); tag.putBoolean("Living", living);
        savePos(tag, "CrowdedHome", crowdedHome);
        savePos(tag, "ObservedBed", observedBed);
        tag.putInt("CrowdingPenalty", crowdingPenalty); tag.putInt("LightPenalty", lightPenalty); tag.putInt("FearPenalty", fearPenalty); tag.putBoolean("ConditionsUnknown", conditionsUnknown);
        tag.putInt("RegenerationTicks", regenerationTicks);
        tag.putLong("ServingDay", servingDay); CompoundTag servings = new CompoundTag(); servingsToday.forEach(servings::putInt); tag.put("ServingsToday", servings);
        tag.putBoolean("Craving", craving); tag.putInt("CravingThreshold", cravingThreshold); tag.putInt("CravingCycle", cravingCycle);
        tag.put("HomeAmenities", homeAmenities.save()); tag.put("WorkAmenities", workAmenities.save()); tag.putLong("AmenityDirtySince", amenityDirtySince);
        ListTag groups = new ListTag(); recentFoods.forEach(value -> { CompoundTag row = new CompoundTag(); row.putString("Food", value); groups.add(row); }); tag.put("RecentFoods", groups);
        if (requestedDish != null) tag.putString("RequestedDish", requestedDish.toString());
        if (lastDish != null) tag.putString("LastDish", lastDish.toString()); tag.putBoolean("RepeatedDish", repeatedDish);
        CompoundTag foods = new CompoundTag(); foodCooldownDays.forEach(foods::putLong); tag.put("FoodDays", foods); tag.put("PositiveDays", saveLongIntMap(positiveByDay)); tag.put("NegativeDays", saveLongIntMap(negativeByDay)); return tag;
    }
    public static ResidentMood load(CompoundTag tag) {
        ResidentMood resident = new ResidentMood(tag.getUUID("Villager"), tag.getUUID("Town"), loadPos(tag, "Home"), loadPos(tag, "Work"));
        resident.needObservation = tag.getCompound("NeedObservation").copy();
        resident.crowdedHome = loadPos(tag, "CrowdedHome");
        resident.observedBed = loadPos(tag, "ObservedBed");
        resident.crowdingPenalty = clamp(tag.getInt("CrowdingPenalty")); resident.lightPenalty = clamp(tag.getInt("LightPenalty")); resident.fearPenalty = clamp(tag.getInt("FearPenalty")); resident.conditionsUnknown = tag.getBoolean("ConditionsUnknown");
        resident.environmentBonus = clamp(tag.getInt("EnvironmentBonus")); resident.socialMoodCap = tag.contains("SocialMoodCap") ? clamp(tag.getInt("SocialMoodCap")) : 100;
        resident.mood = tag.contains("Mood") ? clamp(tag.getInt("Mood")) : 50; resident.satiety = tag.contains("Satiety") ? clamp(tag.getInt("Satiety")) : 50;
        resident.lastServingGameTime = tag.contains("LastServing") ? tag.getLong("LastServing") : Long.MIN_VALUE; resident.lastDailyUpdate = tag.contains("LastDailyUpdate") ? tag.getLong("LastDailyUpdate") : Long.MIN_VALUE;
        resident.displayName = tag.contains("DisplayName") ? tag.getString("DisplayName") : null;
        if (tag.contains("Profession")) resident.profession = tag.getString("Profession");
        resident.regenerationTicks = Math.max(0, Math.min(79, tag.getInt("RegenerationTicks")));
        resident.servingDay = tag.contains("ServingDay") ? tag.getLong("ServingDay") : Long.MIN_VALUE;
        CompoundTag servings = tag.getCompound("ServingsToday"); servings.getAllKeys().forEach(key -> resident.servingsToday.put(key, Math.max(0, servings.getInt(key))));
        resident.craving = tag.getBoolean("Craving"); resident.cravingCycle = tag.getInt("CravingCycle"); resident.cravingThreshold = tag.contains("CravingThreshold") ? Math.max(10, Math.min(99, tag.getInt("CravingThreshold"))) : resident.nextCravingThreshold();
        resident.child = tag.getBoolean("Child"); resident.lastPatDay = tag.contains("LastPatDay") ? tag.getLong("LastPatDay") : Long.MIN_VALUE;
        resident.living = !tag.contains("Living") || tag.getBoolean("Living"); if (tag.contains("HomeAmenities")) resident.homeAmenities = AmenitySnapshot.load(tag.getCompound("HomeAmenities"));
        if (tag.contains("WorkAmenities")) resident.workAmenities = AmenitySnapshot.load(tag.getCompound("WorkAmenities")); resident.amenityDirtySince = tag.getLong("AmenityDirtySince");
        ListTag groups = tag.contains("RecentFoods") ? tag.getList("RecentFoods", Tag.TAG_COMPOUND) : tag.getList("RecentGroups", Tag.TAG_COMPOUND); for (int i = 0; i < groups.size() && i < 4; i++) { String value = groups.getCompound(i).contains("Food") ? groups.getCompound(i).getString("Food") : groups.getCompound(i).getString("Group"); if (!value.isBlank()) resident.recentFoods.addLast(value); }
        if (tag.contains("RequestedDish")) resident.requestedDish = ResourceLocation.tryParse(tag.getString("RequestedDish"));
        if (tag.contains("LastDish")) resident.lastDish = ResourceLocation.tryParse(tag.getString("LastDish")); resident.repeatedDish = tag.getBoolean("RepeatedDish");
        CompoundTag foods = tag.getCompound("FoodDays"); foods.getAllKeys().forEach(key -> resident.foodCooldownDays.put(key, foods.getLong(key)));
        loadLongIntMap(tag.getList("PositiveDays", Tag.TAG_COMPOUND), resident.positiveByDay); loadLongIntMap(tag.getList("NegativeDays", Tag.TAG_COMPOUND), resident.negativeByDay); return resident;
    }
    private static void savePos(CompoundTag tag, String key, GlobalPos pos) { if (pos != null) { CompoundTag row = new CompoundTag(); row.putString("Dimension", pos.dimension().location().toString()); row.putLong("Position", pos.pos().asLong()); tag.put(key, row); } }
    private static GlobalPos loadPos(CompoundTag tag, String key) { if (!tag.contains(key, Tag.TAG_COMPOUND)) return null; CompoundTag row = tag.getCompound(key); ResourceLocation id = ResourceLocation.tryParse(row.getString("Dimension")); return id == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), BlockPos.of(row.getLong("Position"))); }
    private static ListTag saveLongIntMap(Map<Long, Integer> values) { ListTag rows = new ListTag(); values.forEach((day, value) -> { CompoundTag row = new CompoundTag(); row.putLong("Day", day); row.putInt("Value", value); rows.add(row); }); return rows; }
    private static void loadLongIntMap(ListTag rows, Map<Long, Integer> target) { for (int i = 0; i < rows.size(); i++) { CompoundTag row = rows.getCompound(i); target.put(row.getLong("Day"), row.getInt("Value")); } }
}
