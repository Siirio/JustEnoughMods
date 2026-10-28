package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observes exact native Enderman Overhaul commits, storing provenance only on actual native objects. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class EndermanPearlProof {
    private static final String BOUND = "JemQuestSoulBoundV4";
    private static final String THROWN = "JemQuestEnderThrownV4";
    private static final String SUMMONED = "JemQuestAncientSummonedV4";
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> PROTECTED = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("endermanoverhaul", "cant_be_teleported"));
    private static final Set<String> PEARLS = Set.of("soul_pearl", "ancient_pearl", "icy_pearl", "bubble_pearl", "corrupted_pearl");
    private static final Set<String> PETS = Set.of("pet_enderman", "hammerhead_pet_enderman", "axolotl_pet_enderman");
    private static final Set<String> HOODS = Set.of("badlands_hood", "savanna_hood", "snowy_hood");
    private static final int LIMIT = 16;
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private static final class Run {
        final String route;
        final ServerPlayer actor;
        final Entity target;
        final ItemStack item, before;
        final Vec3 position;
        final boolean valid;
        Entity spawned;
        boolean delivered, moved, zeroGravity;
        final List<LivingEntity> frozenTargets = new ArrayList<>();
        Run(String route, ServerPlayer actor, Entity target, ItemStack item, boolean valid) {
            this.route = route; this.actor = actor; this.target = target; this.item = item; before = item.copy();
            position = target == null ? Vec3.ZERO : target.position(); this.valid = valid;
        }
    }
    private EndermanPearlProof() {}
    public static boolean real(Entity entity) {
        return entity instanceof ServerPlayer actor && !(actor instanceof FakePlayer) && !actor.isSpectator()
                && !actor.getAbilities().instabuild && actor.serverLevel().getServer().isSameThread();
    }
    private static String itemId(ItemStack item) { return BuiltInRegistries.ITEM.getKey(item.getItem()).toString(); }
    private static boolean nativeItem(ItemStack item, String name) { return itemId(item).equals("endermanoverhaul:" + name); }
    private static boolean pet(Entity entity) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return type.getNamespace().equals("endermanoverhaul") && PETS.contains(type.getPath());
    }
    private static boolean owned(Entity entity, ServerPlayer actor) {
        return entity instanceof OwnableEntity ownable && actor.getUUID().equals(ownable.getOwnerUUID());
    }
    private static boolean allowed(Entity entity) { return entity != null && entity.isAlive() && !entity.getType().is(PROTECTED); }
    private static void emit(ServerPlayer actor, String action) { QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_" + action); }
    private static Run current() {
        Calls calls = CALLS.get();
        return calls.overflow == 0 && !calls.calls.isEmpty() ? calls.calls.peek() : null;
    }
    public static void begin(String route, Entity actor, Entity target, ItemStack item) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.calls.size() >= LIMIT) { calls.overflow++; return; }
        ServerPlayer player = actor instanceof ServerPlayer actual ? actual : null;
        calls.calls.push(new Run(route, player, target, item,
                real(actor) && (target == null || target.level() == actor.level())));
    }
    public static void beginThrow(Player actor, InteractionHand hand) { begin("throw", actor, null, actor.getItemInHand(hand)); }
    public static void beginProjectile(String route, Projectile projectile, Entity target) {
        Entity actor = projectile.getOwner();
        CompoundTag proof = projectile.getPersistentData().getCompound(THROWN);
        String required = switch (route) {
            case "soul" -> "soul_pearl";
            case "ancient" -> "ancient_pearl";
            case "icy" -> "icy_pearl";
            case "bubble" -> "bubble_pearl";
            default -> "corrupted_pearl";
        };
        boolean eligible = real(actor) && proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor"))
                && proof.getString("item").equals("endermanoverhaul:" + required) && !proof.getBoolean("notified");
        if (route.equals("soul")) eligible &= target != null && proof.hasUUID("bound") && target.getUUID().equals(proof.getUUID("bound")) && allowed(target);
        begin(route, eligible ? actor : null, target == null ? projectile : target, ItemStack.EMPTY);
    }
    public static void beginBlade(ItemStack stack, LivingEntity target, LivingEntity actor) {
        begin("corrupt", nativeItem(stack, "corrupted_blade") && actor.getMainHandItem() == stack && allowed(target) ? actor : null, target, stack);
    }
    public static void beginShield(LivingEntity actor, LivingEntity target) {
        begin("corrupt", nativeItem(actor.getUseItem(), "corrupted_shield") && allowed(target) ? actor : null, target, actor.getUseItem());
    }
    public static void beginRecall(Entity pet, Player actor) {
        CompoundTag proof = pet.getPersistentData().getCompound(SUMMONED);
        boolean eligible = real(actor) && owned(pet, (ServerPlayer)actor) && pet(pet) && pet.isAlive()
                && proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor"))
                && proof.hasUUID("pet") && pet.getUUID().equals(proof.getUUID("pet"));
        begin("recall", eligible ? actor : null, pet, ItemStack.EMPTY);
    }
    private static boolean nativeBoundItem(ItemStack item, Entity target) {
        CompoundTag tag = item.getTag();
        return nativeItem(item, "soul_pearl") && tag != null && tag.contains("BoundEntity") && tag.getInt("BoundEntity") == target.getId()
                && tag.getString("BoundType").equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
    }
    private static boolean bindingKnown(Entity target, ServerPlayer actor) {
        ListTag proofs = target.getPersistentData().getList(BOUND, Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(LIMIT, proofs.size()); index++) {
            CompoundTag proof = proofs.getCompound(index);
            if (proof.hasUUID("actor") && proof.hasUUID("bound") && actor.getUUID().equals(proof.getUUID("actor"))
                    && target.getUUID().equals(proof.getUUID("bound"))) return true;
        }
        return false;
    }
    private static void rememberBinding(Entity target, ServerPlayer actor) {
        if (bindingKnown(target, actor)) return;
        ListTag previous = target.getPersistentData().getList(BOUND, Tag.TAG_COMPOUND), proofs = new ListTag();
        for (int index = Math.max(0, previous.size() - (LIMIT - 1)); index < previous.size(); index++) proofs.add(previous.getCompound(index).copy());
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putUUID("bound", target.getUUID());
        proofs.add(proof); target.getPersistentData().put(BOUND, proofs);
    }
    public static void delivered(ItemStack result, boolean accepted) {
        Run run = current();
        if (run != null && run.valid && run.route.equals("bind") && accepted && allowed(run.target)
                && nativeBoundItem(result, run.target)) run.delivered = true;
    }
    public static void spawned(Entity entity, boolean accepted) {
        Run run = current();
        if (run == null || !run.valid || !accepted || entity.isRemoved() || entity.level() != run.actor.level()) return;
        if (run.route.equals("throw") && entity instanceof Projectile projectile && projectile.getOwner() == run.actor) run.spawned = entity;
        if (run.route.equals("ancient") && pet(entity) && owned(entity, run.actor)) run.spawned = entity;
        if (run.route.equals("recall") && entity instanceof ItemEntity output && nativeItem(output.getItem(), "ancient_pearl")) {
            CompoundTag tag = output.getItem().getTag();
            CompoundTag saved = tag == null ? new CompoundTag() : tag.getCompound("PetEntity");
            if (saved.hasUUID("UUID") && saved.getUUID("UUID").equals(run.target.getUUID())) run.spawned = output;
        }
    }
    public static void moved(Entity target, Vec3 before, boolean accepted) {
        Run run = current();
        if (run != null && run.valid && run.target == target && allowed(target) && accepted
                && target.level() == run.actor.level() && target.position().distanceToSqr(before) > 0.000001) run.moved = true;
    }
    public static void froze(LivingEntity target, int before) {
        Run run = current();
        if (run != null && run.valid && run.route.equals("icy") && target != run.actor && target.isAlive() && target.canFreeze()
                && (target instanceof net.minecraft.world.entity.monster.Enemy || target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == run.actor
                    || run.actor.getLastHurtByMob() == target)
                && target.getTicksFrozen() > before && target.getTicksFrozen() >= target.getTicksRequiredToFreeze()
                && run.frozenTargets.size() < LIMIT) run.frozenTargets.add(target);
    }
    public static void gravity(Entity projectile, float nativeGravity) {
        Run run = current();
        if (run != null && run.valid && run.route.equals("bubble") && run.target == projectile && nativeGravity == 0.0F) run.zeroGravity = true;
    }
    public static void end(boolean success) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Run run = calls.calls.isEmpty() ? null : calls.calls.pop();
        if (calls.calls.isEmpty()) CALLS.remove();
        if (run == null || !run.valid || !success || !real(run.actor)) return;
        switch (run.route) {
            case "bind" -> {
                if (run.delivered && allowed(run.target)) { rememberBinding(run.target, run.actor); emit(run.actor, "soul_pearl_bound"); }
            }
            case "throw" -> {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(run.before.getItem());
                if (run.spawned == null || run.spawned.isRemoved() || !id.getNamespace().equals("endermanoverhaul") || !PEARLS.contains(id.getPath())
                        || run.before.getCount() - run.item.getCount() != 1 || !run.actor.getCooldowns().isOnCooldown(run.before.getItem())) return;
                CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.putString("item", id.toString());
                if (nativeItem(run.before, "soul_pearl")) {
                    CompoundTag tag = run.before.getTag();
                    Entity target = tag == null ? null : run.actor.level().getEntity(tag.getInt("BoundEntity"));
                    if (!allowed(target) || (!nativeBoundItem(run.before, target) || !bindingKnown(target, run.actor))) return;
                    proof.putUUID("bound", target.getUUID());
                }
                run.spawned.getPersistentData().put(THROWN, proof);
            }
            case "soul" -> { if (run.moved) emit(run.actor, "soul_pearl"); }
            case "corrupt" -> { if (run.moved) emit(run.actor, "corrupted_gear"); }
            case "icy" -> {
                if (run.frozenTargets.stream().anyMatch(target -> target.isAlive() && target.canFreeze()
                        && target.level() == run.actor.level() && target.getTicksFrozen() >= target.getTicksRequiredToFreeze())) emit(run.actor, "pearl_control");
            }
            case "bubble" -> {
                if (run.zeroGravity && run.target != null && !run.target.isRemoved() && !run.target.isInWater()
                        && run.target.position().distanceToSqr(run.position) > 0.000001) {
                    run.target.getPersistentData().getCompound(THROWN).putBoolean("notified", true); emit(run.actor, "pearl_control");
                }
            }
            case "ancient" -> {
                if (run.spawned != null && run.spawned.isAlive() && owned(run.spawned, run.actor)) {
                    CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.putUUID("pet", run.spawned.getUUID());
                    run.spawned.getPersistentData().put(SUMMONED, proof); emit(run.actor, "ancient_pearl_summoned");
                }
            }
            case "recall" -> {
                if (run.target.isRemoved() && run.target.getRemovalReason() == Entity.RemovalReason.DISCARDED
                        && run.spawned != null && !run.spawned.isRemoved()) emit(run.actor, "ancient_pearl");
            }
            default -> { }
        }
    }
    @SubscribeEvent
    public static void hoodEquipped(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer actor) || !real(actor) || event.getSlot() != EquipmentSlot.CHEST) return;
        ItemStack actual = actor.getItemBySlot(EquipmentSlot.CHEST);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(actual.getItem());
        if (id.getNamespace().equals("endermanoverhaul") && HOODS.contains(id.getPath()) && !ItemStack.isSameItemSameTags(event.getFrom(), actual)
                && ItemStack.isSameItemSameTags(event.getTo(), actual)) emit(actor, "hood");
    }
}
