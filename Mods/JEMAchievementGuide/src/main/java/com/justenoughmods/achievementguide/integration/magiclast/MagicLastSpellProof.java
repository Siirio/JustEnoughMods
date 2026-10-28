package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
/** Native cast generations, not the transient SpellData object returned by its getter. */
public final class MagicLastSpellProof {
    private static final String SHOT = "JemQuestNativeSpellV4";
    private static final Map<MagicData, Generation> CASTS = new IdentityHashMap<>();
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Attempt> attempts = new ArrayDeque<>(); final ArrayDeque<Cast> casts = new ArrayDeque<>(); final ArrayDeque<Damage> damage = new ArrayDeque<>(); final ArrayDeque<Float> heals = new ArrayDeque<>(); final ArrayDeque<Projectile> nativeEffects = new ArrayDeque<>(); int attemptOverflow, castOverflow, damageOverflow, healOverflow, effectOverflow; }
    private record Attempt(AbstractSpell spell, int level, ServerPlayer actor, CastSource source) {}
    private static final class Generation { final UUID token = UUID.randomUUID(); final ServerPlayer actor; final AbstractSpell spell; final int level; final CastSource source; final boolean blade; boolean valid = true; Generation(ServerPlayer actor, AbstractSpell spell, int level, CastSource source, boolean blade) { this.actor = actor; this.spell = spell; this.level = level; this.source = source; this.blade = blade; } }
    private static final class Cast { final Generation generation; final Level world; final List<Projectile> projectiles = new ArrayList<>(); boolean outcome, frost; Cast(Generation generation, Level world) { this.generation = generation; this.world = world; } }
    private record Damage(Entity target, DamageSource source, float vitality, int frozen, Generation generation) {}
    private MagicLastSpellProof() {}
    public static void attemptBegin(AbstractSpell spell, int level, ServerPlayer actor, CastSource source) { State state = STATE.get(); if (state.attemptOverflow > 0 || state.attempts.size() >= MagicLastProof.LIMIT) state.attemptOverflow++; else state.attempts.push(new Attempt(spell, level, actor, source)); }
    public static void attemptEnd() { State state = STATE.get(); if (state.attemptOverflow > 0) state.attemptOverflow--; else if (!state.attempts.isEmpty()) state.attempts.pop(); clean(state); }
    public static void initiated(MagicData data, ServerPlayer actor, AbstractSpell spell, int level, CastSource source) {
        Generation old = CASTS.remove(data); if (old != null) old.valid = false;
        if (!MagicLastProof.real(actor) || level <= 0 || !data.isCasting() || !data.getCastingSpellId().equals(spell.getSpellId()) || data.getCastingSpellLevel() != level || data.getCastSource() != source) return;
        State state = STATE.get(); Attempt attempt = state.attemptOverflow == 0 && !state.attempts.isEmpty() ? state.attempts.peek() : null;
        boolean blade = attempt != null && attempt.actor == actor && attempt.spell == spell && attempt.level == level && attempt.source == source;
        if (CASTS.size() >= 256) { for (Generation generation : CASTS.values()) generation.valid = false; CASTS.clear(); }
        CASTS.put(data, new Generation(actor, spell, level, source, blade));
    }
    public static void reset(MagicData data) { Generation generation = CASTS.remove(data); if (generation != null) generation.valid = false; }
    public static void castBegin(AbstractSpell spell, Level world, int level, ServerPlayer actor, CastSource source) {
        State state = STATE.get(); if (state.castOverflow > 0 || state.casts.size() >= MagicLastProof.LIMIT) { state.castOverflow++; return; }
        MagicData data = MagicData.getPlayerMagicData(actor); Generation generation = CASTS.get(data);
        if (!MagicLastProof.real(actor) || actor.level() != world || !data.isCasting() || generation == null || !generation.valid || generation.actor != actor || generation.spell != spell || generation.level != level || generation.source != source || !data.getCastingSpellId().equals(spell.getSpellId())) generation = null;
        state.casts.push(new Cast(generation, world));
    }
    private static Cast current() { State state = STATE.get(); return state.castOverflow == 0 && !state.casts.isEmpty() ? state.casts.peek() : null; }
    public static void castEnd() {
        State state = STATE.get(); if (state.castOverflow > 0) { state.castOverflow--; return; } Cast run = state.casts.isEmpty() ? null : state.casts.pop(); clean(state);
        if (run == null || run.generation == null || !run.generation.valid || !MagicLastProof.real(run.generation.actor)) return;
        for (Projectile projectile : run.projectiles) { CompoundTag proof = projectile.getPersistentData().getCompound(SHOT); if (proof.hasUUID("generation") && proof.getUUID("generation").equals(run.generation.token)) proof.putBoolean("committed", true); }
        if (run.frost) MagicLastProof.emit(run.generation.actor, "v3_magic_fire_ice");
        if (run.outcome && run.generation.blade) MagicLastProof.emit(run.generation.actor, "v3_magic_blade_cast");
    }
    public static void spawned(Entity entity, boolean accepted) {
        Cast run = current(); if (!accepted || run == null || run.generation == null || !run.generation.valid || entity.level() != run.world) return; Generation generation = run.generation;
        if (entity instanceof Projectile projectile && projectile.getOwner() == generation.actor && run.projectiles.size() < MagicLastProof.LIMIT) { CompoundTag proof = new CompoundTag(); proof.putUUID("actor", generation.actor.getUUID()); proof.putUUID("generation", generation.token); proof.putString("spell", generation.spell.getSpellId()); proof.putBoolean("blade", generation.blade); projectile.getPersistentData().put(SHOT, proof); run.projectiles.add(projectile); }
        else if (entity instanceof LivingEntity && entity instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon summon && summon.getSummoner() == generation.actor) run.outcome = true;
    }
    public static void outcome(Entity target) { Cast run = current(); if (run != null && run.generation != null && run.generation.valid && target.level() == run.world) run.outcome = true; }
    public static void blockOutcome(Level world) { Cast run = current(); if (run != null && run.world == world && run.generation != null && run.generation.valid) run.outcome = true; }
    private static float vitality(Entity entity) { return entity instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : Float.NaN; }
    public static void damageBegin(Entity target, DamageSource source) { State state = STATE.get(); if (state.damageOverflow > 0 || state.damage.size() >= MagicLastProof.LIMIT) state.damageOverflow++; else { Cast run = current(); state.damage.push(new Damage(target, source, vitality(target), target.getTicksFrozen(), run == null ? null : run.generation)); } }
    public static void damageEnd(boolean accepted) {
        State state = STATE.get(); if (state.damageOverflow > 0) { state.damageOverflow--; return; } Damage run = state.damage.isEmpty() ? null : state.damage.pop(); clean(state);
        if (run == null || !accepted || !(vitality(run.target) < run.vitality) || !(run.source instanceof SpellDamageSource nativeSource) || !MagicLastProof.real(run.source.getEntity())) return;
        ServerPlayer actor = (ServerPlayer)run.source.getEntity(); if (run.generation != null && run.generation.actor == actor && nativeSource.spell() == run.generation.spell) outcome(run.target);
        String spell = nativeSource.spell().getSpellId();
        if (spell.equals("irons_spellbooks:ray_of_frost") && run.generation != null && run.generation.valid && run.generation.actor == actor && run.target.isAlive() && run.target.isFullyFrozen() && run.target.getTicksFrozen() > run.frozen) { Cast cast = current(); if (cast != null && cast.generation == run.generation) cast.frost = true; }
        if (!(run.source.getDirectEntity() instanceof Projectile projectile) || projectile.getOwner() != actor) return;
        CompoundTag proof = projectile.getPersistentData().getCompound(SHOT);
        if (!proof.getBoolean("committed") || !proof.hasUUID("actor") || !proof.getUUID("actor").equals(actor.getUUID()) || !proof.getString("spell").equals(spell)) return;
        if (proof.getBoolean("blade")) MagicLastProof.emit(actor, "v3_magic_blade_cast");
        if (nativeEffect(projectile) && spell.equals("irons_spellbooks:fireball") && projectile.getClass().getName().equals("io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball")) MagicLastProof.emit(actor, "v3_magic_fire_ice");
        if (nativeEffect(projectile) && spell.equals("irons_spellbooks:chain_lightning") && projectile.getClass().getName().equals("io.redspace.ironsspellbooks.entity.spells.ChainLightning")) {
            ListTag victims = proof.getList("victims", Tag.TAG_COMPOUND); boolean exists = false;
            for (int index = 0; index < Math.min(MagicLastProof.LIMIT, victims.size()); index++) if (victims.getCompound(index).hasUUID("target") && victims.getCompound(index).getUUID("target").equals(run.target.getUUID())) exists = true;
            if (!exists && victims.size() < MagicLastProof.LIMIT) { CompoundTag row = new CompoundTag(); row.putUUID("target", run.target.getUUID()); victims.add(row); proof.put("victims", victims); }
            if (victims.size() >= 2) MagicLastProof.emit(actor, "v3_magic_fire_ice");
        }
    }
    public static void nativeBegin(Projectile projectile) { State state = STATE.get(); if (state.effectOverflow > 0 || state.nativeEffects.size() >= MagicLastProof.LIMIT) state.effectOverflow++; else state.nativeEffects.push(projectile); }
    public static void nativeEnd() { State state = STATE.get(); if (state.effectOverflow > 0) state.effectOverflow--; else if (!state.nativeEffects.isEmpty()) state.nativeEffects.pop(); clean(state); }
    private static boolean nativeEffect(Projectile projectile) { State state = STATE.get(); return state.effectOverflow == 0 && !state.nativeEffects.isEmpty() && state.nativeEffects.peek() == projectile; }
    public static void healBegin(LivingEntity target) { State state = STATE.get(); if (state.healOverflow > 0 || state.heals.size() >= MagicLastProof.LIMIT) state.healOverflow++; else state.heals.push(target.getHealth()); }
    public static void healEnd(LivingEntity target) { State state = STATE.get(); if (state.healOverflow > 0) state.healOverflow--; else if (!state.heals.isEmpty() && target.getHealth() > state.heals.pop()) outcome(target); clean(state); }
    public static void clear() { for (Generation generation : CASTS.values()) generation.valid = false; CASTS.clear(); STATE.remove(); }
    private static void clean(State state) { if (state.attempts.isEmpty() && state.casts.isEmpty() && state.damage.isEmpty() && state.heals.isEmpty() && state.nativeEffects.isEmpty()) STATE.remove(); }
}
