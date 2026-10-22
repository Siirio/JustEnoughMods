package com.siirio.jemworldbosstiers.compat.cataclysm;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Native formula inputs are transformed before addition; markers belong to one hurt invocation. */
public final class NativeBossDamage {
    private static final ThreadLocal<Deque<Call>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Resolution>> RESOLUTIONS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Invocation>> INVOCATIONS = ThreadLocal.withInitial(ArrayDeque::new);

    private NativeBossDamage() { }

    private static Optional<BossProfile> profile(LivingEntity boss) {
        if (boss.level().isClientSide || boss.getPersistentData().getBoolean("jem:event_spawned")) return Optional.empty();
        return BalanceRegistry.boss(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
    }

    public static boolean supports(LivingEntity boss) { return profile(boss).isPresent(); }

    public static double attackInput(LivingEntity boss) {
        double factor = flatFactor(boss);
        return boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * factor;
    }

    public static double flatFactor(LivingEntity boss) {
        var profile = profile(boss);
        if (profile.isEmpty()) return 1;
        int tier = EncounterScaler.ensureScaled(boss, profile.get());
        double factor = profile.get().specialDamageMultiplier() == null ? 1
                : profile.get().specialDamageMultiplier().valueAt(tier);
        factor *= HostedEncounters.damageFactor(boss);
        if (HostedEncounters.isHosted(boss)) factor *= 0.88D;
        return factor;
    }

    public static double fixedFactor(LivingEntity boss) {
        double factor = flatFactor(boss);
        var profile = profile(boss);
        if (profile.isPresent() && profile.get().scalesWithWorldTier()) {
            factor *= com.siirio.jemworldbosstiers.balance.BossTierScaling.boost(EncounterScaler.encounterTier(boss));
        }
        return factor;
    }

    public static boolean hurt(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (profile(boss).isEmpty()) return target.hurt(source, amount);
        return scopedHit(target, source, amount, true, null);
    }

    public static boolean physicalHit(LivingEntity target, DamageSource source, float amount, Object actionToken) {
        return scopedHit(target, source, amount, false, actionToken);
    }

    private static boolean scopedHit(LivingEntity target, DamageSource source, float amount,
                                     boolean adapted, Object actionToken) {
        Deque<Call> calls = CALLS.get();
        int depth = INVOCATIONS.get().size();
        calls.push(new Call(target, source, adapted, actionToken));
        try {
            return target.hurt(source, amount);
        } finally {
            calls.pop();
            if (calls.isEmpty()) CALLS.remove();
            Deque<Invocation> invocations = INVOCATIONS.get();
            while (invocations.size() > depth) invocations.pop();
            if (invocations.isEmpty()) INVOCATIONS.remove();
        }
    }

    public static void beginInvocation(LivingEntity target, DamageSource source) {
        Invocation parent = INVOCATIONS.get().peek();
        if (target instanceof net.minecraft.world.entity.player.Player && parent != null && parent.player
                && parent.target == target && parent.source == source) {
            INVOCATIONS.get().push(new Invocation(target, source, parent.adapted, false, parent.actionToken));
            return;
        }
        claimInvocation(target, source, false);
    }

    public static void beginPlayerInvocation(LivingEntity target, DamageSource source) {
        claimInvocation(target, source, true);
    }

    private static void claimInvocation(LivingEntity target, DamageSource source, boolean player) {
        Call call = CALLS.get().peek();
        if (call == null && INVOCATIONS.get().isEmpty()) {
            CALLS.remove();
            INVOCATIONS.remove();
            return;
        }
        boolean claimed = call != null && !call.bound && call.target == target && call.source == source;
        if (claimed) call.bound = true;
        INVOCATIONS.get().push(new Invocation(target, source, claimed && call.adapted, player,
                claimed ? call.actionToken : null));
        if (call == null) CALLS.remove();
    }

    public static boolean invocationIs(Object actionToken) {
        Deque<Invocation> invocations = INVOCATIONS.get();
        Invocation invocation = invocations.peek();
        if (invocations.isEmpty()) INVOCATIONS.remove();
        return actionToken != null && invocation != null && invocation.actionToken == actionToken;
    }

    public static void endInvocation() {
        Deque<Invocation> invocations = INVOCATIONS.get();
        if (!invocations.isEmpty()) invocations.pop();
        if (invocations.isEmpty()) INVOCATIONS.remove();
    }

    // Called around each virtual actuallyHurt dispatch, including nested hits with the same source.
    public static void beginResolution(LivingEntity target, DamageSource source) {
        Deque<Invocation> invocations = INVOCATIONS.get();
        Invocation invocation = invocations.peek();
        RESOLUTIONS.get().push(new Resolution(target, source, invocation != null && invocation.adapted
                && invocation.target == target && invocation.source == source));
        if (invocations.isEmpty()) INVOCATIONS.remove();
    }

    public static void endResolution() {
        Deque<Resolution> resolutions = RESOLUTIONS.get();
        resolutions.pop();
        if (resolutions.isEmpty()) RESOLUTIONS.remove();
    }

    public static boolean consume(LivingEntity target, DamageSource source) {
        Deque<Resolution> resolutions = RESOLUTIONS.get();
        if (resolutions.isEmpty()) { RESOLUTIONS.remove(); return false; }
        Resolution resolution = resolutions.peek();
        if (!resolution.adapted || resolution.target != target || resolution.source != source) return false;
        resolution.adapted = false;
        return true;
    }

    private static final class Resolution {
        final LivingEntity target;
        final DamageSource source;
        boolean adapted;
        Resolution(LivingEntity target, DamageSource source, boolean adapted) {
            this.target = target; this.source = source; this.adapted = adapted;
        }
    }

    private record Invocation(LivingEntity target, DamageSource source, boolean adapted, boolean player,
                              Object actionToken) { }

    private static final class Call {
        final LivingEntity target;
        final DamageSource source;
        final boolean adapted;
        final Object actionToken;
        boolean bound;
        Call(LivingEntity target, DamageSource source, boolean adapted, Object actionToken) {
            this.target = target; this.source = source; this.adapted = adapted; this.actionToken = actionToken;
        }
    }
}
