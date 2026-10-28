package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.Event;

public final class CombatEffectProof {
    private static final String POTION = "JemQuestTrialPotionV4", APPLIED = "JemQuestTrialAppliedV4";
    private static final Set<String> EFFECTS = Set.of("trials:infested", "trials:oozing", "trials:winded", "trials:weaving");
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State {
        final ArrayDeque<Throw> throws_ = new ArrayDeque<>(); final ArrayDeque<CompoundTag> applications = new ArrayDeque<>(); final ArrayDeque<Outcome> outcomes = new ArrayDeque<>();
        int throwOverflow, applyOverflow, outcomeOverflow;
    }
    private static final class Throw {
        final Entity actor; final ItemStack item, before; ThrownPotion projectile;
        Throw(Entity actor, ItemStack item) { this.actor = actor; this.item = item; before = item.copy(); }
    }
    private record Outcome(LivingEntity target, Entity attacker, Event event, boolean death) {}
    private CombatEffectProof() {}
    public static void clear() { STATE.remove(); }
    private static String effect(MobEffect effect) { return BuiltInRegistries.MOB_EFFECT.getKey(effect).toString(); }
    public static void throwBegin(Player actor, ItemStack item) {
        State state = STATE.get(); if (state.throwOverflow > 0 || state.throws_.size() >= CombatToolProof.LIMIT) { state.throwOverflow++; return; } state.throws_.push(new Throw(actor, item));
    }
    public static void thrown(Entity projectile, boolean accepted) {
        State state = STATE.get(); if (state.throwOverflow > 0 || state.throws_.isEmpty()) return;
        Throw run = state.throws_.peek(); if (accepted && projectile instanceof ThrownPotion actual && actual.getOwner() == run.actor) run.projectile = actual;
    }
    public static void throwEnd(boolean accepted) {
        State state = STATE.get(); if (state.throwOverflow > 0) { state.throwOverflow--; return; }
        Throw run = state.throws_.isEmpty() ? null : state.throws_.pop(); clean(state);
        if (run == null || !accepted || !CombatToolProof.real(run.actor) || run.projectile == null || run.projectile.isRemoved() || run.before.getCount() - run.item.getCount() != 1) return;
        ListTag rows = new ListTag();
        for (MobEffectInstance effect : PotionUtils.getMobEffects(run.before)) if (EFFECTS.contains(effect(effect.getEffect())) && rows.size() < 4) {
            CompoundTag row = new CompoundTag(); row.putString("effect", effect(effect.getEffect())); row.putInt("amplifier", effect.getAmplifier()); rows.add(row);
        }
        if (!rows.isEmpty()) { CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.put("effects", rows); run.projectile.getPersistentData().put(POTION, proof); }
    }
    public static void applyBegin(Entity source) {
        State state = STATE.get(); if (state.applyOverflow > 0 || state.applications.size() >= CombatToolProof.LIMIT) { state.applyOverflow++; return; }
        CompoundTag proof = source.getPersistentData().getCompound(POTION).copy();
        Entity owner = source instanceof Projectile shot ? shot.getOwner() : source instanceof AreaEffectCloud cloud ? cloud.getOwner() : null;
        if (!CombatToolProof.real(owner) || !proof.hasUUID("actor") || !proof.getUUID("actor").equals(owner.getUUID())) proof = new CompoundTag();
        state.applications.push(proof);
    }
    public static void applyEnd() { State state = STATE.get(); if (state.applyOverflow > 0) state.applyOverflow--; else if (!state.applications.isEmpty()) state.applications.pop(); clean(state); }
    public static void cloud(Entity cloud, boolean accepted) {
        State state = STATE.get(); if (accepted && cloud instanceof AreaEffectCloud && state.applyOverflow == 0 && !state.applications.isEmpty() && !state.applications.peek().isEmpty()) cloud.getPersistentData().put(POTION, state.applications.peek().copy());
    }
    public static void changed(LivingEntity target, MobEffectInstance effect, boolean accepted) {
        String key = effect(effect.getEffect()); if (!accepted || !EFFECTS.contains(key)) return;
        target.getPersistentData().getCompound(APPLIED).remove(key);
        State state = STATE.get(); if (!(target instanceof Mob) || state.applyOverflow > 0 || state.applications.isEmpty()) return;
        CompoundTag source = state.applications.peek(); MobEffectInstance installed = target.getEffect(effect.getEffect());
        if (!source.hasUUID("actor") || installed == null || installed.getDuration() <= 0 || installed.getAmplifier() != effect.getAmplifier()) return;
        ListTag rows = source.getList("effects", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(4, rows.size()); index++) if (rows.getCompound(index).getString("effect").equals(key) && rows.getCompound(index).getInt("amplifier") == installed.getAmplifier()) {
            CompoundTag proof = new CompoundTag(); proof.putUUID("actor", source.getUUID("actor")); proof.putInt("amplifier", installed.getAmplifier()); proof.putLong("until", target.level().getGameTime() + installed.getDuration());
            CompoundTag all = target.getPersistentData().getCompound(APPLIED); all.put(key, proof); target.getPersistentData().put(APPLIED, all); break;
        }
    }
    private static UUID owner(LivingEntity target, String key) {
        if (!(target instanceof Mob)) return null;
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new net.minecraft.resources.ResourceLocation(key));
        MobEffectInstance actual = target.getEffect(effect); CompoundTag proof = target.getPersistentData().getCompound(APPLIED).getCompound(key);
        return actual != null && actual.getDuration() > 0 && proof.hasUUID("actor") && proof.getLong("until") >= target.level().getGameTime() && actual.getAmplifier() == proof.getInt("amplifier") ? proof.getUUID("actor") : null;
    }
    public static void outcomeBegin(LivingEntity target, Entity attacker, Event event, boolean death) {
        State state = STATE.get(); if (state.outcomeOverflow > 0 || state.outcomes.size() >= CombatToolProof.LIMIT) { state.outcomeOverflow++; return; } state.outcomes.push(new Outcome(target, attacker, event, death));
    }
    public static void outcomeEnd() { State state = STATE.get(); if (state.outcomeOverflow > 0) state.outcomeOverflow--; else if (!state.outcomes.isEmpty()) state.outcomes.pop(); clean(state); }
    public static void spawned(Entity output, boolean accepted) {
        State state = STATE.get(); if (!accepted || !output.isAlive() || state.outcomeOverflow > 0 || state.outcomes.isEmpty()) return;
        Outcome run = state.outcomes.peek(); if (run.event.isCancelable() && run.event.isCanceled()) return;
        if (!run.death && output.getType() == EntityType.SILVERFISH) CombatLifeProof.hurtNotice(run.target, owner(run.target, "trials:infested"), "trial_effects");
        if (run.death && output.getType() == EntityType.SLIME) CombatLifeProof.deathNotice(run.target, owner(run.target, "trials:oozing"), "trial_effects");
    }
    public static void web(Level level, BlockPos pos, BlockState state, boolean accepted) {
        State calls = STATE.get(); if (!accepted || !state.is(Blocks.COBWEB) || !level.getBlockState(pos).is(Blocks.COBWEB) || calls.outcomeOverflow > 0 || calls.outcomes.isEmpty()) return;
        Outcome run = calls.outcomes.peek();
        if (!run.death && run.attacker instanceof LivingEntity holder && !(run.event.isCancelable() && run.event.isCanceled())) CombatLifeProof.hurtNotice(run.target, owner(holder, "trials:weaving"), "trial_effects");
    }
    private static void clean(State state) { if (state.throws_.isEmpty() && state.applications.isEmpty() && state.outcomes.isEmpty()) STATE.remove(); }
}
