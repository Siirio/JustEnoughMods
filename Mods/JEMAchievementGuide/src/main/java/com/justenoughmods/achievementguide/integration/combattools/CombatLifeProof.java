package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;

/** Final native health/death acceptance for native callback producers. */
public final class CombatLifeProof {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State {
        final ArrayDeque<Hurt> hurts = new ArrayDeque<>(); final ArrayDeque<Death> deaths = new ArrayDeque<>(); final ArrayDeque<Attack> attacks = new ArrayDeque<>();
        int hurtOverflow, deathOverflow, attackOverflow;
    }
    private record Notice(UUID actor, String action) {}
    private record Hurt(LivingEntity target, float vitality, List<Notice> notices) {}
    private record Death(LivingEntity target, boolean before, DamageSource source, List<Notice> notices) {}
    private static final class Attack {
        final ServerPlayer actor; final Entity target; final ItemStack tool; final float vitality;
        CriticalHitEvent critical; float modifier;
        Attack(ServerPlayer actor, Entity target) { this.actor = actor; this.target = target; tool = actor == null ? ItemStack.EMPTY : actor.getMainHandItem(); vitality = vitality(target); }
    }
    private CombatLifeProof() {}
    public static void clear() { STATE.remove(); }
    private static float vitality(Entity target) { return target instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : Float.NaN; }
    private static void notify(LivingEntity target, List<Notice> notices) {
        if (target.getServer() == null) return;
        for (Notice notice : notices) {
            ServerPlayer actor = target.getServer().getPlayerList().getPlayer(notice.actor);
            if (actor != null) CombatToolProof.emit(actor, notice.action);
        }
    }
    public static void hurtBegin(LivingEntity target) {
        State state = STATE.get(); if (state.hurtOverflow > 0 || state.hurts.size() >= CombatToolProof.LIMIT) { state.hurtOverflow++; return; }
        state.hurts.push(new Hurt(target, vitality(target), new ArrayList<>()));
    }
    public static void hurtNotice(LivingEntity target, UUID actor, String action) {
        State state = STATE.get();
        if (actor != null && state.hurtOverflow == 0 && !state.hurts.isEmpty() && state.hurts.peek().target == target && state.hurts.peek().notices.size() < CombatToolProof.LIMIT)
            state.hurts.peek().notices.add(new Notice(actor, action));
    }
    public static void hurtEnd(LivingEntity target, boolean accepted) {
        State state = STATE.get(); if (state.hurtOverflow > 0) { state.hurtOverflow--; return; }
        Hurt hurt = state.hurts.isEmpty() ? null : state.hurts.pop();
        if (hurt != null && hurt.target == target && accepted && vitality(target) < hurt.vitality) { notify(target, hurt.notices); CombatRuneProof.committed(target); }
        clean(state);
    }
    public static void deathBegin(LivingEntity target, boolean dead, DamageSource source) {
        State state = STATE.get(); if (state.deathOverflow > 0 || state.deaths.size() >= CombatToolProof.LIMIT) { state.deathOverflow++; return; }
        state.deaths.push(new Death(target, dead, source, new ArrayList<>()));
    }
    public static void deathNotice(LivingEntity target, UUID actor, String action) {
        State state = STATE.get();
        if (actor != null && state.deathOverflow == 0 && !state.deaths.isEmpty() && state.deaths.peek().target == target && state.deaths.peek().notices.size() < CombatToolProof.LIMIT)
            state.deaths.peek().notices.add(new Notice(actor, action));
    }
    public static void deathEnd(LivingEntity target, boolean dead) {
        State state = STATE.get(); if (state.deathOverflow > 0) { state.deathOverflow--; return; }
        Death death = state.deaths.isEmpty() ? null : state.deaths.pop();
        if (death != null && death.target == target && !death.before && dead) {
            notify(target, death.notices);
            CombatTrialProof.defeated(target, death.source);
            CombatCorpseProof.defeated(target, death.source);
        }
        clean(state);
    }
    public static void attackBegin(Entity actor, Entity target) {
        State state = STATE.get(); if (state.attackOverflow > 0 || state.attacks.size() >= CombatToolProof.LIMIT) { state.attackOverflow++; return; }
        state.attacks.push(new Attack(CombatToolProof.real(actor) ? (ServerPlayer)actor : null, target));
    }
    public static void smash(CriticalHitEvent event, float before) {
        State state = STATE.get(); if (state.attackOverflow > 0 || state.attacks.isEmpty()) return;
        Attack attack = state.attacks.peek();
        if (attack.actor != null && event.getEntity() == attack.actor && event.getTarget() == attack.target && event.isVanillaCritical()
                && attack.actor.getMainHandItem() == attack.tool && event.getDamageModifier() > before) { attack.critical = event; attack.modifier = before; }
    }
    public static void attackEnd(Entity actor) {
        State state = STATE.get(); if (state.attackOverflow > 0) { state.attackOverflow--; return; }
        Attack attack = state.attacks.isEmpty() ? null : state.attacks.pop();
        if (attack != null && attack.actor == actor && attack.critical != null && attack.critical.getResult() != Event.Result.DENY
                && attack.critical.getDamageModifier() > attack.modifier && vitality(attack.target) < attack.vitality) {
            CompoundTag proof = attack.tool.getTagElement(CombatCraftProof.MACE);
            if (proof != null && proof.getBoolean("committed") && proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID())
                    && CombatToolProof.item(attack.tool).equals("trials:mace")) CombatToolProof.emit(attack.actor, "mace_smash");
        }
        clean(state);
    }
    private static void clean(State state) { if (state.hurts.isEmpty() && state.deaths.isEmpty() && state.attacks.isEmpty()) STATE.remove(); }
}
