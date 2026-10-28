package com.justenoughmods.achievementguide.integration.battle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.phys.Vec3;

/** Narrow invocation receipts for the remaining named native artifact functions. */
public final class WorldBattleExtraProof {
    public static final class Use {
        public final ServerPlayer actor;
        public final ItemStack stack;
        public final String kind;
        public final List<Entity> spawned = new ArrayList<>();
        public final Vec3 motion;
        public final Vec3 position;
        public final int damage;
        public boolean cooldown;
        public boolean positiveHit;
        public boolean pulled;
        public final boolean valid;
        Use(Entity source, ItemStack stack, Item nativeItem, String kind) {
            actor = source instanceof ServerPlayer player ? player : null;
            this.stack = stack; this.kind = kind; motion = source == null ? Vec3.ZERO : source.getDeltaMovement(); position = source == null ? Vec3.ZERO : source.position(); damage = stack.getDamageValue();
            valid = BattleQuestProof.real(source) && stack.getItem() == nativeItem && !stack.isEmpty()
                    && (actor.getMainHandItem() == stack || actor.getOffhandItem() == stack || actor.getUseItem() == stack
                        || kind.equals("bloom") && actor.getItemBySlot(EquipmentSlot.CHEST) == stack);
        }
    }
    private static final class Calls { final ArrayDeque<Use> rows = new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private WorldBattleExtraProof() {}
    public static void begin(Entity actor, ItemStack stack, Item nativeItem, String kind) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) { calls.overflow++; return; }
        calls.rows.push(new Use(actor, stack, nativeItem, kind));
    }
    public static Use current() {
        Calls calls = CALLS.get();
        return calls.overflow == 0 && !calls.rows.isEmpty() && calls.rows.peek().valid ? calls.rows.peek() : null;
    }
    public static void spawned(Entity entity, boolean accepted) {
        Use use = current();
        if (use == null || !accepted || entity.isRemoved() || entity.level() != use.actor.level()
                || use.actor.serverLevel().getEntity(entity.getUUID()) != entity || use.spawned.size() >= 64) return;
        if (!use.spawned.contains(entity)) use.spawned.add(entity);
    }
    public static void cooldown(ItemCooldowns cooldowns, Item item, int ticks) {
        Use use = current();
        if (use != null && use.actor.getCooldowns() == cooldowns && use.stack.getItem() == item && ticks > 0
                && cooldowns.isOnCooldown(item)) use.cooldown = true;
    }
    public static void damage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float before) {
        Use use = current();
        if (use != null && source.getEntity() == use.actor && BattleQuestProof.hostile(target) && !target.isAlliedTo(use.actor)
                && Float.isFinite(before) && Float.isFinite(target.getHealth()) && target.getHealth() < before) use.positiveHit = true;
    }
    public static void velocity(Entity target, Vec3 before) {
        Use use = current();
        if (use == null || !use.kind.equals("guard") || !(target instanceof LivingEntity living)
                || !BattleQuestProof.hostile(living) || target.isAlliedTo(use.actor) || target.level() != use.actor.level()) return;
        Vec3 change = target.getDeltaMovement().subtract(before), inward = use.actor.position().subtract(target.position());
        if (finite(change) && change.lengthSqr() > 1.0E-10 && change.dot(inward) > 0) use.pulled = true;
    }
    private static boolean finite(Vec3 value) { return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z); }
    private static long count(Use use, Set<String> names) {
        return use.spawned.stream().filter(e -> !e.isRemoved() && names.contains(e.getClass().getSimpleName())
                && (!(e instanceof Projectile projectile) || projectile.getOwner() == use.actor)).count();
    }
    public static void end() {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Use use = calls.rows.poll(); if (calls.rows.isEmpty()) CALLS.remove();
        if (use == null || !use.valid || !BattleQuestProof.real(use.actor)) return;
        String action = "";
        switch (use.kind) {
            case "infernal" -> { if (use.positiveHit && use.cooldown) action = "v3_combat_artifact_earth"; }
            case "annihilator" -> { if (use.positiveHit && use.cooldown) action = "v3_combat_artifact_charge"; }
            case "guard" -> { if (use.pulled) action = "v3_combat_artifact_earth"; }
            case "wrath" -> { if (count(use, Set.of("Cursed_Sandstorm_Entity")) == 3) action = "v3_combat_artifact_ranged"; }
            case "wither" -> { if (use.cooldown && count(use, Set.of("Wither_Howitzer_Entity", "Wither_Missile_Entity")) == 1) action = "v3_combat_artifact_ranged"; }
            case "void_shoulder" -> { if (use.cooldown && count(use, Set.of("Void_Howitzer_Entity")) == 1) action = "v3_combat_artifact_ranged"; }
            case "laser" -> { if (count(use, Set.of("Laser_Beam_Entity")) == 1) action = "v3_combat_artifact_ranged"; }
            case "spear" -> { if (count(use, Set.of("Sandstorm_Projectile")) == 1) action = "v3_combat_artifact_sand"; }
            case "bloom" -> { if (use.cooldown && count(use, Set.of("Amethyst_Cluster_Projectile_Entity")) == 8) action = "v3_combat_abyss_kit"; }
            case "soul" -> {
                if (use.cooldown && count(use, Set.of("Phantom_Halberd_Entity")) > 0) {
                    if (WorldBattleEquipmentOrigins.mode(use.actor, use.stack, "soul_render", 2)) action = "v3_combat_artifact_stances";
                } else if (use.cooldown && finite(use.actor.getDeltaMovement()) && use.actor.getDeltaMovement().distanceToSqr(use.motion) > 1.0E-8)
                    WorldBattleExtraMovement.begin(use, "soul");
            }
            case "bulwark" -> {
                if (use.cooldown && finite(use.actor.getDeltaMovement()) && use.actor.getDeltaMovement().distanceToSqr(use.motion) > 1.0E-8)
                    WorldBattleExtraMovement.begin(use, "bulwark");
            }
            case "tidal_tentacle" -> {
                for (Entity entity : use.spawned) {
                    if (!entity.isRemoved() && entity instanceof com.github.L_Ender.cataclysm.entity.projectile.Tidal_Tentacle_Entity tentacle
                            && use.actor.getUUID().equals(tentacle.getCreatorEntityUUID())
                            && WorldBattleEquipmentOrigins.mode(use.actor, use.stack, "tidal_claws", 2)) { action = "v3_combat_artifact_stances"; break; }
                }
            }
            case "tidal_hook" -> {
                for (Entity entity : use.spawned) if (entity instanceof com.github.L_Ender.cataclysm.entity.projectile.Tidal_Hook_Entity hook
                        && hook.getOwner() == use.actor) WorldBattleExtraMovement.hook(use.actor, use.stack, hook);
            }
            default -> { }
        }
        if (!action.isEmpty()) BattleQuestProof.emit(use.actor, action);
    }
    public static void clear() { CALLS.remove(); WorldBattleExtraMovement.clear(); }
}
