package com.justenoughmods.achievementguide.integration.worldmisc;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** User-approved zero-food route, existing native damage and hunger owners stay untouched. */
public final class WorldHungerGuardProof {
    private static final String ORIGIN = "jem_native_void_squid_meal_v4";
    private static final ResourceLocation EFFECT = new ResourceLocation("aquamirae", "devouring_curse");
    private static final Map<ServerPlayer, MobEffectInstance> MEALS = new WeakHashMap<>();
    private static final class Attempt {
        final Player actor; final DamageSource source; final float amount; final float health;
        boolean nativeProtected;
        Attempt(Player actor, DamageSource source, float amount) { this.actor = actor; this.source = source; this.amount = amount; health = actor.getHealth(); }
    }
    private static final class Calls { final ArrayDeque<Attempt> rows = new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private WorldHungerGuardProof() {}
    public static void eaten(ServerPlayer actor, ItemStack before, ItemStack actual, int initialCount) {
        if (!WorldNativeQuestProof.real(actor) || !WorldNativeQuestProof.item(before, "aquamirae:void_squid")
                || initialCount - actual.getCount() != 1) return;
        MobEffectInstance acquired = nativeEffect(actor);
        if (acquired != null) {
            actor.getPersistentData().putUUID(ORIGIN, actor.getUUID());
            MEALS.put(actor, acquired);
        }
    }
    private static boolean acquired(ServerPlayer actor) {
        if (!MEALS.containsKey(actor)) restore(actor);
        MobEffectInstance effect = MEALS.get(actor);
        if (effect != null && nativeEffect(actor) == effect) return true;
        forget(actor); return false;
    }
    private static MobEffectInstance nativeEffect(ServerPlayer actor) {
        var effect = BuiltInRegistries.MOB_EFFECT.get(EFFECT);
        MobEffectInstance actual = effect == null ? null : actor.getEffect(effect);
        return actual != null && (actual.isInfiniteDuration() || actual.getDuration() > 0) ? actual : null;
    }
    public static void restore(ServerPlayer actor) {
        var saved = actor.getPersistentData(); MobEffectInstance actual = nativeEffect(actor);
        if (actual != null && saved.hasUUID(ORIGIN) && saved.getUUID(ORIGIN).equals(actor.getUUID())) MEALS.put(actor, actual);
        else forget(actor);
    }
    public static void removed(ServerPlayer actor, MobEffectInstance removed) {
        if (EFFECT.equals(BuiltInRegistries.MOB_EFFECT.getKey(removed.getEffect()))) forget(actor);
    }
    private static void forget(ServerPlayer actor) { MEALS.remove(actor); actor.getPersistentData().remove(ORIGIN); }
    public static void begin(Player actor, DamageSource source, float amount) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) { calls.overflow++; return; }
        calls.rows.push(new Attempt(actor, source, amount));
    }
    public static void nativeProtection(Player actor, DamageSource source, boolean protectedByNativeOwner) {
        Calls calls = CALLS.get(); Attempt attempt = calls.overflow > 0 ? null : calls.rows.peek();
        if (attempt != null && attempt.actor == actor && attempt.source == source && protectedByNativeOwner
                && actor instanceof ServerPlayer player && WorldNativeQuestProof.real(player)
                && player.getFoodData().getFoodLevel() == 0 && acquired(player)) attempt.nativeProtected = true;
    }
    public static void end(Player actor, DamageSource source, boolean acceptedDamage) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Attempt attempt = calls.rows.poll(); if (calls.rows.isEmpty()) CALLS.remove();
        if (attempt == null || attempt.actor != actor || attempt.source != source || !attempt.nativeProtected || acceptedDamage
                || !Float.isFinite(attempt.amount) || attempt.amount <= 0 || source.is(DamageTypes.GENERIC_KILL)
                || !(actor instanceof ServerPlayer player) || !WorldNativeQuestProof.real(player) || !acquired(player)
                || player.getFoodData().getFoodLevel() != 0 || player.getHealth() != attempt.health) return;
        WorldNativeQuestProof.emit(player, "v3_era_hunger_guard");
    }
    public static void clear(ServerPlayer actor) { MEALS.remove(actor); }
    public static void clear() { MEALS.clear(); CALLS.remove(); }
}
