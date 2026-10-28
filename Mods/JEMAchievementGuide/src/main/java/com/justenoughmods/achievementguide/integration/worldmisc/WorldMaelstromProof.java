package com.justenoughmods.achievementguide.integration.worldmisc;

import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;

/** Same actual paid Tear effect and committed native destination, never a teleport owner. */
public final class WorldMaelstromProof {
    private static final String ORIGIN = "jem_native_maelstrom_drink_v4";
    private static final ResourceLocation EFFECT = new ResourceLocation("aquamirae", "maelstrom_omen");
    private static final Map<ServerPlayer, MobEffectInstance> DRINKS = new WeakHashMap<>();
    private record Drink(ItemStack stack, int count, LivingEntity entity) {}
    private static final class Calls { final ArrayDeque<Drink> rows = new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private WorldMaelstromProof() {}
    public static void beginDrink(ItemStack stack, LivingEntity entity) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) { calls.overflow++; return; }
        calls.rows.push(new Drink(stack, stack.getCount(), entity));
    }
    public static void endDrink(ItemStack stack, LivingEntity entity) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Drink call = calls.rows.poll(); if (calls.rows.isEmpty()) CALLS.remove();
        if (call != null && call.stack == stack && call.entity == entity && entity instanceof ServerPlayer actor
                && call.count - stack.getCount() == 1) drank(actor);
    }
    public static void drank(ServerPlayer actor) {
        if (!WorldNativeQuestProof.real(actor)) return;
        MobEffectInstance nativeEffect = nativeEffect(actor);
        if (nativeEffect != null) {
            actor.getPersistentData().putUUID(ORIGIN, actor.getUUID());
            DRINKS.put(actor, nativeEffect);
        }
    }
    public static void arrived(ServerPlayer actor, ServerLevel source, ResourceKey<Level> destination) {
        if (!active(actor)) return;
        MobEffectInstance drink = DRINKS.get(actor);
        // Exact installed AquamiraeRegion.THE_MAELSTROM registration, not an inferred snow dimension.
        if (!WorldNativeQuestProof.real(actor) || drink == null || !destination.location().toString().equals("aquamirae:the_maelstrom")
                || source == actor.serverLevel() || actor.serverLevel() != source.getServer().getLevel(destination)
                || !actor.serverLevel().dimension().equals(destination)) return;
        forget(actor); WorldNativeQuestProof.emit(actor, "v3_era_eye_of_the_blizzard");
    }
    public static boolean active(ServerPlayer actor) {
        if (actor == null) return false;
        if (!DRINKS.containsKey(actor)) restore(actor);
        MobEffectInstance drink = DRINKS.get(actor);
        if (drink != null && nativeEffect(actor) == drink) return true;
        forget(actor); return false;
    }
    private static MobEffectInstance nativeEffect(ServerPlayer actor) {
        var effect = BuiltInRegistries.MOB_EFFECT.get(EFFECT);
        MobEffectInstance actual = effect == null ? null : actor.getEffect(effect);
        return actual != null && (actual.isInfiniteDuration() || actual.getDuration() > 0) ? actual : null;
    }
    /** Native player NBT owns both the origin and the effect, runtime caches own neither. */
    public static void restore(ServerPlayer actor) {
        var saved = actor.getPersistentData(); MobEffectInstance actual = nativeEffect(actor);
        if (actual != null && saved.hasUUID(ORIGIN) && saved.getUUID(ORIGIN).equals(actor.getUUID())) DRINKS.put(actor, actual);
        else forget(actor);
    }
    public static void removed(ServerPlayer actor, MobEffectInstance removed) {
        if (EFFECT.equals(BuiltInRegistries.MOB_EFFECT.getKey(removed.getEffect()))) forget(actor);
    }
    private static void forget(ServerPlayer actor) { DRINKS.remove(actor); actor.getPersistentData().remove(ORIGIN); }
    public static void clear(ServerPlayer actor) { DRINKS.remove(actor); }
    public static void clear() { DRINKS.clear(); CALLS.remove(); }
}
