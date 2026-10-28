package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Observed native rune identity and native object lineage, never bow tuning or mechanics. */
public final class CombatRuneProof {
    public static final String BOW = "JemQuestRuneBowV4", SHOT = "JemQuestRuneShotV4";
    private static final Map<Object, Origin> OBJECTS = new java.util.IdentityHashMap<>();
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Scope> calls = new ArrayDeque<>(); final ArrayDeque<Float> health = new ArrayDeque<>(); int overflow, healthOverflow; }
    public record Origin(ServerLevel level, UUID actor, CompoundTag identity) {}
    private record Scope(Origin origin, boolean direct) {}
    private CombatRuneProof() {}
    public static Origin bow(Entity owner, ItemStack bow) {
        CompoundTag proof = bow.getTagElement(BOW);
        if (!CombatToolProof.real(owner) || proof == null || !proof.getBoolean("committed") || !proof.hasUUID("actor") || !proof.hasUUID("token")
                || !owner.getUUID().equals(proof.getUUID("actor")) || !CombatToolProof.item(bow).equals(proof.getString("bow"))
                || !bow.getOrCreateTag().getCompound("simplybows_upgrades").getString("rune").equals(proof.getString("rune"))) return null;
        return new Origin((ServerLevel)owner.level(), owner.getUUID(), proof.copy());
    }
    public static void beginBow(Entity owner, ItemStack bow) { enter(bow(owner, bow), false); }
    public static void enter(Origin origin, boolean direct) { Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) calls.overflow++; else calls.calls.push(new Scope(origin, direct)); }
    public static void leave() { Calls calls = CALLS.get(); if (calls.overflow > 0) calls.overflow--; else if (!calls.calls.isEmpty()) calls.calls.pop(); clean(calls); }
    private static Scope current() { Calls calls = CALLS.get(); return calls.overflow == 0 && !calls.calls.isEmpty() ? calls.calls.peek() : new Scope(null, false); }
    public static void remember(Object nativeObject) { Origin origin = current().origin; if (origin != null) { if (OBJECTS.size() >= 512) OBJECTS.clear(); OBJECTS.put(nativeObject, origin); } }
    public static void object(Object nativeObject, String runes) { Origin origin = OBJECTS.get(nativeObject); enter(origin, origin != null && allowed(origin, runes)); }
    public static void branch(String runes) { Origin origin = current().origin; enter(origin, origin != null && allowed(origin, runes)); }
    public static void adopt(Object nativeObject) { Calls calls = CALLS.get(); Origin origin = OBJECTS.get(nativeObject); if (origin != null && calls.overflow == 0 && !calls.calls.isEmpty()) { Scope scope = calls.calls.pop(); calls.calls.push(new Scope(origin, scope.direct)); } }
    private static boolean allowed(Origin origin, String runes) { return runes.equals("*") || java.util.Arrays.asList(runes.split(",")).contains(origin.identity.getString("rune")); }
    public static Origin control() { Scope scope = current(); return scope.direct ? scope.origin : null; }
    public static void spawned(Entity entity, boolean accepted) {
        Scope scope = current(); Origin origin = scope.origin;
        if (accepted && entity instanceof Projectile projectile && origin != null && entity.level() == origin.level && projectile.getOwner() != null && projectile.getOwner().getUUID().equals(origin.actor)) {
            CompoundTag proof = origin.identity.copy(); proof.putBoolean("direct", scope.direct); entity.getPersistentData().put(SHOT, proof);
        }
        if (accepted && entity instanceof net.minecraft.world.entity.item.ItemEntity && scope.direct) committed(entity);
    }
    public static void arrow(Entity entity) {
        CompoundTag proof = entity.getPersistentData().getCompound(SHOT);
        Origin origin = entity.level() instanceof ServerLevel level && proof.hasUUID("actor") && proof.hasUUID("token") && entity instanceof Projectile projectile && projectile.getOwner() != null && projectile.getOwner().getUUID().equals(proof.getUUID("actor")) ? new Origin(level, proof.getUUID("actor"), proof.copy()) : null;
        enter(origin, origin != null && proof.getBoolean("direct"));
    }
    public static void markChanged(Entity arrow, String runes) { CompoundTag proof = arrow.getPersistentData().getCompound(SHOT); if (!proof.isEmpty() && (runes.equals("*") || java.util.Arrays.asList(runes.split(",")).contains(proof.getString("rune")))) proof.putBoolean("direct", true); }
    public static void deliveredBow(Entity owner, ItemStack bow, String runes) { Origin origin = bow(owner, bow); enter(origin, origin != null && allowed(origin, runes)); }
    public static void committed(Entity target) {
        Scope scope = current(); Origin origin = scope.origin;
        if (origin == null || !scope.direct || target.level() != origin.level) return;
        ServerPlayer actor = origin.level.getServer().getPlayerList().getPlayer(origin.actor);
        if (CombatToolProof.real(actor)) CombatToolProof.emit(actor, "bow_rune");
    }
    public static void healBegin(LivingEntity target) { Calls calls = CALLS.get(); if (calls.healthOverflow > 0 || calls.health.size() >= CombatToolProof.LIMIT) calls.healthOverflow++; else calls.health.push(target.getHealth()); }
    public static void healEnd(LivingEntity target) { Calls calls = CALLS.get(); if (calls.healthOverflow > 0) calls.healthOverflow--; else if (!calls.health.isEmpty() && target.getHealth() > calls.health.pop()) committed(target); clean(calls); }
    public static void clear() { OBJECTS.clear(); CALLS.remove(); }
    private static void clean(Calls calls) { if (calls.calls.isEmpty() && calls.health.isEmpty()) CALLS.remove(); }
}
