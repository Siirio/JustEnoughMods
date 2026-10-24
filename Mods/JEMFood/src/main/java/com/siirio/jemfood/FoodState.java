package com.siirio.jemfood;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** One server-owned bundle, durable trait budgets and exact native-instance identities. */
public final class FoodState {
    public static final String KEY = "JemFoodBlessingV2";
    private static final String LEGACY = "JemFoodQualityV1";
    private static final UUID VARIETY = UUID.fromString("766d2410-48f2-4223-bc78-d23860a9e004");
    private static final Map<UUID, FoodState> STATES = new HashMap<>();
    final Map<String, Long> meals = new LinkedHashMap<>();
    final Map<String, Long> cooldowns = new HashMap<>();
    final Map<String, Integer> counters = new HashMap<>();
    final Map<MobEffect, MobEffectInstance> owned = new HashMap<>();
    String dish = "";
    long expires;
    long lastDamage;
    long windUntil;
    double windPower;
    boolean internal;
    private long lastAir = Long.MIN_VALUE;
    private FoodNetwork.Snapshot sent;

    private FoodState() {}
    public static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    public static FoodState get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> load(player));
    }
    public FoodCatalog.Dish active(ServerPlayer player) {
        if (expires <= now(player) || !player.hasEffect(JEMFood.BLESSING.get())) return null;
        var id = ResourceLocation.tryParse(dish);
        return id == null ? null : FoodCatalog.find(id);
    }
    public double value(ServerPlayer player, String trait) {
        var active = active(player);
        return active != null && active.traits().contains(trait) ? FoodCatalog.trait(trait).value(active.tier()) : 0;
    }
    public boolean ready(String trait, long now) { return cooldowns.getOrDefault(trait, 0L) <= now; }
    public void cooldown(String trait, long now, long duration) { cooldowns.put(trait, now + duration); }
    public boolean threshold(String trait, int count) {
        int accumulated = counters.getOrDefault(trait, 0) + 1;
        counters.put(trait, Math.min(count, accumulated));
        if (accumulated < count) return false;
        counters.put(trait, 0);
        return true;
    }

    public void consume(ServerPlayer player, ResourceLocation id) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return;
        long now = now(player);
        var next = FoodCatalog.find(id);
        long carriedWindUntil = windUntil;
        double carriedWindPower = windPower;
        double triggeredWind = 0;
        // Recovery talents react to any genuine other food, including unclassified native food.
        var previous = active(player);
        if (previous != null && !dish.equals(id.toString())) {
            double reserve = value(player, "reserve");
            if (reserve > 0 && ready("reserve", now)) {
                var food = player.getFoodData();
                food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + (float) reserve));
                cooldown("reserve", now, 1200);
            }
            double wind = value(player, "secondwind");
            if (wind > 0 && ready("secondwind", now)) {
                triggeredWind = wind;
            }
        }
        if (next != null) {
            if (next.variety()) meals.put(id.toString(), now);
            if (next.tier() > 0 && !(dish.equals(id.toString()) && previous != null
                    && expires - now > previous.duration() / 5)) {
                clearBundle(player);
                if (!next.traits().isEmpty()) {
                    internal = true;
                    boolean added;
                    try { added = player.addEffect(new MobEffectInstance(JEMFood.BLESSING.get(), next.duration(), 0, false, false, true)); }
                    finally { internal = false; }
                    if (added) {
                        dish = id.toString();
                        expires = now + next.duration();
                        if (next.traits().contains("hearth")) cooldowns.putIfAbsent("hearth", now + (long) value(player, "hearth"));
                    }
                }
            }
        }
        if (active(player) != null) {
            windUntil = carriedWindUntil;
            windPower = carriedWindPower;
            if (triggeredWind > 0) {
                windUntil = now + 100;
                windPower = triggeredWind;
                cooldown("secondwind", now, 1200);
            }
        }
        pulse(player);
        save(player);
    }

    public void pulse(ServerPlayer player) {
        long now = now(player);
        owned.entrySet().removeIf(entry -> player.getEffect(entry.getKey()) != entry.getValue());
        if (active(player) == null && !dish.isEmpty()) clearBundle(player);
        var active = active(player);
        if (active != null) {
            for (String name : active.traits()) {
                var trait = FoodCatalog.trait(name);
                if (trait.bridge()) bridge(player, trait, (int) trait.value(active.tier()), now);
            }
            int air = (int) value(player, "deepbreath");
            if (air > 0 && player.isUnderWater() && now % air == 0 && lastAir != now) {
                player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 1));
                lastAir = now;
            }
            int hearth = (int) value(player, "hearth");
            if (hearth > 0 && ready("hearth", now) && now - lastDamage >= 300
                    && player.getFoodData().getFoodLevel() >= 16 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1);
                cooldown("hearth", now, hearth);
            }
        }
        meals.entrySet().removeIf(entry -> entry.getValue() > now || now - entry.getValue() >= 72000);
        while (meals.size() > 5) meals.remove(meals.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey());
        modifier(player, Attributes.MOVEMENT_SPEED, VARIETY, meals.size() >= 5 ? 0.05 : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        attribute(player, Attributes.ATTACK_SPEED, "fervor", value(player, "fervor"), AttributeModifier.Operation.MULTIPLY_BASE);
        attribute(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), "highstep", value(player, "highstep"), AttributeModifier.Operation.ADDITION);
        attribute(player, Attributes.KNOCKBACK_RESISTANCE, "steadfast", value(player, "steadfast"), AttributeModifier.Operation.ADDITION);
        attribute(player, Attributes.MAX_HEALTH, "heartroot", value(player, "heartroot"), AttributeModifier.Operation.ADDITION);
        // Second Wind remains attached to the bundle that triggered it, never to an independent manager.
        attribute(player, Attributes.MOVEMENT_SPEED, "secondwind", active != null && now < windUntil ? windPower : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        var snapshot = FoodNetwork.snapshot(player, this);
        if (sent == null || !snapshot.dish().equals(sent.dish()) || snapshot.variety() != sent.variety() || now % 20 == 0) {
            FoodNetwork.send(player, snapshot); sent = snapshot;
        }
        if (now % 20 == 0) save(player);
    }

    private void bridge(ServerPlayer player, FoodCatalog.Trait trait, int duration, long now) {
        // Preferred IDs remain dormant until their exact installed semantics and ownership are audited.
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(trait.fallback());
        if (effect == null) throw new IllegalStateException("Missing Minecraft fallback " + trait.fallback());
        if (!ready(trait.id(), now) || player.hasEffect(effect)) return;
        int boundedDuration = (int) Math.min(duration, expires - now);
        if (boundedDuration <= 0) return;
        internal = true;
        try {
            var instance = new MobEffectInstance(effect, boundedDuration, 0, false, false, false);
            if (player.addEffect(instance)) {
                owned.put(effect, player.getEffect(effect));
                cooldown(trait.id(), now, trait.id().equals("stonehide") ? 2400 : 1200);
            }
        } finally { internal = false; }
    }

    public static boolean owns(ServerPlayer player, MobEffect effect) {
        FoodState state = STATES.get(player.getUUID());
        return state != null && state.owned.containsKey(effect) && state.owned.get(effect) == player.getEffect(effect);
    }
    public static void external(ServerPlayer player, MobEffectInstance incoming) {
        FoodState state = STATES.get(player.getUUID());
        if (state != null && !state.internal) state.detach(player, incoming.getEffect());
    }
    private void detach(ServerPlayer player, MobEffect effect) {
        var instance = owned.remove(effect);
        if (instance == null || player.getEffect(effect) != instance) return;
        internal = true;
        try { player.removeEffect(effect); }
        finally { internal = false; }
    }
    public void clearBundle(ServerPlayer player) {
        for (var effect : java.util.List.copyOf(owned.keySet())) detach(player, effect);
        internal = true;
        try { player.removeEffect(JEMFood.BLESSING.get()); }
        finally { internal = false; }
        dish = "";
        expires = 0;
        windUntil = 0;
        for (String trait : new String[]{"heartroot", "steadfast", "highstep", "fervor", "secondwind"}) {
            Attribute attr = switch (trait) {
                case "heartroot" -> Attributes.MAX_HEALTH;
                case "steadfast" -> Attributes.KNOCKBACK_RESISTANCE;
                case "highstep" -> ForgeMod.STEP_HEIGHT_ADDITION.get();
                case "fervor" -> Attributes.ATTACK_SPEED;
                default -> Attributes.MOVEMENT_SPEED;
            };
            attribute(player, attr, trait, 0, AttributeModifier.Operation.ADDITION);
        }
    }
    private static void attribute(ServerPlayer player, Attribute attr, String trait, double amount, AttributeModifier.Operation operation) {
        modifier(player, attr, UUID.nameUUIDFromBytes(("jemfood:" + trait).getBytes(StandardCharsets.UTF_8)), amount, operation);
    }
    private static void modifier(ServerPlayer player, Attribute attr, UUID id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attr);
        if (instance == null) return;
        var current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount && current.getOperation() == operation) return;
        float previousMaximum = player.getMaxHealth();
        if (current != null) instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "JEM Food", amount, operation));
        if (attr == Attributes.MAX_HEALTH && player.getMaxHealth() < previousMaximum && player.getHealth() > player.getMaxHealth())
            player.setHealth(player.getMaxHealth());
    }

    private static FoodState load(ServerPlayer player) {
        FoodState state = new FoodState();
        CompoundTag saved = player.getPersistentData().getCompound(KEY);
        long now = now(player);
        if (!saved.getBoolean("MigratedV1")) {
            CompoundTag old = player.getPersistentData().getCompound(LEGACY);
            readMeals(old.getCompound("Meals"), state, now);
            // Do not import an old native bundle as a new blessing or grant absorption again.
            reclaim(player, old.getCompound("Owned"), state.owned);
            for (MobEffect effect : java.util.List.copyOf(state.owned.keySet())) state.detach(player, effect);
        }
        readMeals(saved.getCompound("Meals"), state, now);
        state.dish = saved.getString("Dish");
        state.expires = saved.getLong("Expires");
        if (state.expires > now + 4800) state.expires = now;
        state.lastDamage = Math.min(now, saved.getLong("LastDamage"));
        state.windUntil = Math.min(now + 100, saved.getLong("WindUntil"));
        state.windPower = Math.max(0, Math.min(0.08, saved.getDouble("WindPower")));
        CompoundTag cooldowns = saved.getCompound("Cooldowns");
        for (String trait : cooldowns.getAllKeys())
            if (FoodCatalog.trait(trait) != null) state.cooldowns.put(trait, Math.min(now + 2400, Math.max(0, cooldowns.getLong(trait))));
        CompoundTag counters = saved.getCompound("Counters");
        for (String trait : counters.getAllKeys())
            if (FoodCatalog.trait(trait) != null) state.counters.put(trait, Math.max(0, Math.min(12, counters.getInt(trait))));
        reclaim(player, saved.getCompound("Owned"), state.owned);
        state.save(player);
        return state;
    }
    private static void readMeals(CompoundTag meals, FoodState state, long now) {
        for (String key : meals.getAllKeys()) {
            var id = ResourceLocation.tryParse(key);
            var dish = id == null ? null : FoodCatalog.find(id);
            long time = meals.getLong(key);
            if (dish != null && dish.variety() && time <= now && now - time < 72000) state.meals.put(key, time);
        }
    }
    private static void reclaim(ServerPlayer player, CompoundTag saved, Map<MobEffect, MobEffectInstance> owned) {
        for (String key : saved.getAllKeys()) {
            var id = ResourceLocation.tryParse(key);
            var effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            var active = effect == null ? null : player.getEffect(effect);
            if (active == null) continue;
            var fingerprint = saved.getCompound(key).copy();
            int delta = fingerprint.getInt("Duration") - active.getDuration();
            fingerprint.putInt("Duration", active.getDuration());
            if (delta >= 0 && delta <= 1 && active.save(new CompoundTag()).equals(fingerprint)) owned.put(effect, active);
        }
    }
    public void save(ServerPlayer player) {
        CompoundTag saved = new CompoundTag();
        saved.putInt("Schema", 2);
        saved.putBoolean("MigratedV1", true);
        saved.putString("Dish", dish);
        saved.putLong("Expires", expires);
        saved.putLong("LastDamage", lastDamage);
        saved.putLong("WindUntil", windUntil);
        saved.putDouble("WindPower", windPower);
        CompoundTag mealTag = new CompoundTag();
        meals.forEach(mealTag::putLong);
        saved.put("Meals", mealTag);
        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach(cooldownTag::putLong);
        saved.put("Cooldowns", cooldownTag);
        CompoundTag counterTag = new CompoundTag();
        counters.forEach(counterTag::putInt);
        saved.put("Counters", counterTag);
        CompoundTag ownedTag = new CompoundTag();
        owned.forEach((effect, instance) -> {
            if (player.getEffect(effect) == instance) ownedTag.put(ForgeRegistries.MOB_EFFECTS.getKey(effect).toString(), instance.save(new CompoundTag()));
        });
        saved.put("Owned", ownedTag);
        player.getPersistentData().put(KEY, saved);
    }
    public static void logout(ServerPlayer player) {
        FoodState state = STATES.remove(player.getUUID());
        if (state != null) state.save(player);
    }
    public static void forget(ServerPlayer player) { STATES.remove(player.getUUID()); }
    public static void stop() { STATES.clear(); }
}
