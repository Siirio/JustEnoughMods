package com.siirio.jemcompat.feature.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One server-owned bundle. Native external effects always win a same-ID collision. */
public final class FoodEvents {
    private static final String KEY = "JemFoodQualityV1";
    private static final UUID VARIETY = UUID.fromString("766d2410-48f2-4223-bc78-d23860a9e004");
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final long WINDOW = 72000;
    private static boolean internal;
    private FoodEvents() {}

    private static final class State {
        String dish = "";
        int remaining;
        int absorptionWindow;
        float spentAbsorption;
        float lastAbsorption;
        boolean cleared;
        final Map<MobEffect, MobEffectInstance> owned = new HashMap<>();
        final Map<String, Long> meals = new HashMap<>();
        int sentCount = -1;
    }

    public static void beforeExternalAdd(ServerPlayer player, MobEffectInstance incoming) {
        if (internal) return;
        State state = STATES.get(player.getUUID());
        if (state == null) return;
        var owned = state.owned.get(incoming.getEffect());
        if (owned != null && owned == player.getEffect(incoming.getEffect())) detach(player, state, incoming.getEffect());
    }

    @SubscribeEvent
    public static void consumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();
        if (item.getFoodProperties(player) == null) return;
        consumedDish(player, ForgeRegistries.ITEMS.getKey(item.getItem()));
    }

    /** Invoked only after native completed-item or verified native block-bite consumption. */
    public static void consumedDish(ServerPlayer player, ResourceLocation id) {
        var dish = FoodCatalog.find(id);
        if (dish == null) return;
        State state = state(player);
        for (var effect : java.util.List.copyOf(state.owned.keySet())) detach(player, state, effect);
        state.dish = dish.id().toString();
        state.remaining = dish.duration();
        state.cleared = false;
        state.meals.put(state.dish, now(player));
        if (dish.effects().stream().anyMatch(e -> e.effect() == MobEffects.ABSORPTION))
            state.absorptionWindow = Math.max(state.absorptionWindow, dish.duration());
        applyMissing(player, state);
        variety(player, state);
        save(player, state);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        State state = state(player);
        accountAbsorption(player, state);
        state.owned.entrySet().removeIf(entry -> player.getEffect(entry.getKey()) != entry.getValue());
        if (state.absorptionWindow > 0 && --state.absorptionWindow == 0) state.spentAbsorption = 0;
        if (state.cleared || state.remaining > 0 && --state.remaining == 0) {
            for (var effect : java.util.List.copyOf(state.owned.keySet())) detach(player, state, effect);
            state.dish = "";
            state.remaining = 0;
            state.cleared = false;
        }
        if (state.remaining > 0) applyMissing(player, state);
        // Pruning is bounded by the curated eligible-ID set, never an entity/world scan.
        variety(player, state);
        save(player, state);
    }

    private static void applyMissing(ServerPlayer player, State state) {
        var id = ResourceLocation.tryParse(state.dish);
        var dish = id == null ? null : FoodCatalog.find(id);
        if (dish == null) return;
        for (var component : dish.effects()) {
            MobEffect effect = component.effect();
            if (effect == null || player.hasEffect(effect)) continue;
            // Some native systems hold raw absorption without an effect. Its scalar has no source ownership.
            if (effect == MobEffects.ABSORPTION && player.getAbsorptionAmount() > 0F) continue;
            float before = player.getAbsorptionAmount();
            internal = true;
            boolean added;
            try { added = player.addEffect(new MobEffectInstance(effect, state.remaining, component.amplifier(), false, false, true)); }
            finally { internal = false; }
            if (!added) continue;
            state.owned.put(effect, player.getEffect(effect));
            if (effect == MobEffects.ABSORPTION) {
                float capacity = 4F * (component.amplifier() + 1);
                // Vanilla grants the full capacity on insertion. Only the unspent allowance belongs to food.
                player.setAbsorptionAmount(before + Math.max(0F, capacity - state.spentAbsorption));
                state.lastAbsorption = player.getAbsorptionAmount();
            }
        }
    }

    private static void accountAbsorption(ServerPlayer player, State state) {
        if (state.owned.get(MobEffects.ABSORPTION) == player.getEffect(MobEffects.ABSORPTION) && state.owned.containsKey(MobEffects.ABSORPTION)) {
            float current = player.getAbsorptionAmount();
            state.spentAbsorption += Math.max(0F, state.lastAbsorption - current);
            state.lastAbsorption = current;
        }
    }

    private static void detach(ServerPlayer player, State state, MobEffect effect) {
        accountAbsorption(player, state);
        var owned = state.owned.remove(effect);
        if (owned == null || player.getEffect(effect) != owned) return;
        internal = true;
        try { player.removeEffect(effect); }
        finally { internal = false; }
        // If another mod cancels removal, leave it external and never remove it on expiry/replacement.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removed(MobEffectEvent.Remove event) {
        if (internal || event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        State state = STATES.get(player.getUUID());
        if (state != null && state.owned.containsKey(event.getEffect()) && state.owned.get(event.getEffect()) == event.getEffectInstance()) {
            state.cleared = true;
            save(player, state);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        State state = state(player);
        accountAbsorption(player, state);
        state.dish = "";
        state.remaining = 0;
        state.owned.clear();
        save(player, state);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var old = event.getOriginal().getPersistentData().getCompound(KEY).copy();
        if (event.isWasDeath()) { old.remove("Dish"); old.remove("Remaining"); old.remove("Owned"); }
        player.getPersistentData().put(KEY, old);
        STATES.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = state(player);
            variety(player, state);
        }
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.remove(player.getUUID());
            if (state != null) { accountAbsorption(player, state); save(player, state); }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { STATES.clear(); }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> {
            State state = new State();
            var saved = player.getPersistentData().getCompound(KEY);
            state.dish = saved.getString("Dish");
            state.remaining = Math.max(0, Math.min(36000, saved.getInt("Remaining")));
            state.cleared = saved.getBoolean("Cleared");
            state.absorptionWindow = Math.max(0, Math.min(36000, saved.getInt("AbsorptionWindow")));
            state.spentAbsorption = Math.max(0F, saved.getFloat("SpentAbsorption"));
            state.lastAbsorption = player.getAbsorptionAmount();
            var meals = saved.getCompound("Meals");
            for (String item : meals.getAllKeys()) {
                var id = ResourceLocation.tryParse(item);
                if (id != null && FoodCatalog.find(id) != null) state.meals.put(item, meals.getLong(item));
            }
            var owned = saved.getCompound("Owned");
            for (String effectId : owned.getAllKeys()) {
                var id = ResourceLocation.tryParse(effectId);
                var effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
                var active = effect == null ? null : player.getEffect(effect);
                if (active != null) {
                    var fingerprint = owned.getCompound(effectId).copy();
                    // Forge PlayerTick.END and native entity serialization may straddle one native decrement.
                    int delta = fingerprint.getInt("Duration") - active.getDuration();
                    fingerprint.putInt("Duration", active.getDuration());
                    if (delta >= 0 && delta <= 1 && active.save(new CompoundTag()).equals(fingerprint)) state.owned.put(effect, active);
                }
            }
            return state;
        });
    }

    private static long now(ServerPlayer player) { return player.getServer().overworld().getGameTime(); }
    private static void variety(ServerPlayer player, State state) {
        long now = now(player);
        state.meals.entrySet().removeIf(entry -> entry.getValue() > now || now - entry.getValue() >= WINDOW);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (state.meals.size() >= 5 && speed.getModifier(VARIETY) == null)
                speed.addTransientModifier(new AttributeModifier(VARIETY, "JEM dietary variety", 0.05D, AttributeModifier.Operation.MULTIPLY_BASE));
            else if (state.meals.size() < 5 && speed.getModifier(VARIETY) != null) speed.removeModifier(VARIETY);
        }
        if (state.sentCount != state.meals.size()) { FoodNetwork.send(player, state.meals.size()); state.sentCount = state.meals.size(); }
    }
    private static void save(ServerPlayer player, State state) {
        var tag = new CompoundTag();
        tag.putInt("Schema", 1);
        tag.putString("Dish", state.dish);
        tag.putInt("Remaining", state.remaining);
        tag.putBoolean("Cleared", state.cleared);
        tag.putInt("AbsorptionWindow", state.absorptionWindow);
        tag.putFloat("SpentAbsorption", state.spentAbsorption);
        var meals = new CompoundTag();
        state.meals.forEach(meals::putLong);
        tag.put("Meals", meals);
        var owned = new CompoundTag();
        state.owned.forEach((effect, instance) -> { if (player.getEffect(effect) == instance) owned.put(ForgeRegistries.MOB_EFFECTS.getKey(effect).toString(), instance.save(new CompoundTag())); });
        tag.put("Owned", owned);
        player.getPersistentData().put(KEY, tag);
    }
}
