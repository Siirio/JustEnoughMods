package com.siirio.jemworldbosstiers.compat.irons;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/** Native healing is settled once after defense, independently for each nested hit. */
public final class SiphonHealing {
    private static final ThreadLocal<Deque<Hit>> HITS = new ThreadLocal<>();

    private SiphonHealing() { }

    public static void beginHit(LivingEntity target, DamageSource source) {
        Deque<Hit> hits = HITS.get();
        if (hits == null) {
            hits = new ArrayDeque<>();
            HITS.set(hits);
        }
        hits.push(new Hit(target, source));
    }

    public static boolean defer(LivingEntity target, DamageSource source, LivingEntity caster, float nativeShare) {
        Deque<Hit> hits = HITS.get();
        Hit hit = hits == null ? null : hits.peek();
        if (hit == null || hit.target != target || hit.source != source) return false;
        // Multiple native callbacks cannot create multiple replacement heals for the same hit.
        if (hit.caster == null) {
            hit.caster = caster;
            hit.nativeShare = nativeShare;
        }
        return true;
    }

    public static void finishHit(LivingEntity target, boolean completed) {
        Deque<Hit> hits = HITS.get();
        Hit hit = hits.pop();
        if (hit.target != target) throw new IllegalStateException("Unbalanced lifesteal hurt boundary");
        float healthLoss = Math.max(0.0F, hit.health - Math.max(0.0F, target.getHealth()));
        float absorptionLoss = Math.max(0.0F, hit.absorption - Math.max(0.0F, target.getAbsorptionAmount()));
        for (Hit parent : hits) {
            if (parent.target == target) {
                parent.childHealthLoss += healthLoss;
                parent.childAbsorptionLoss += absorptionLoss;
                break;
            }
        }
        if (hits.isEmpty()) HITS.remove();
        float ownLoss = Math.max(0.0F, healthLoss - hit.childHealthLoss)
                + Math.max(0.0F, absorptionLoss - hit.childAbsorptionLoss);
        if (completed && hit.caster != null && ownLoss > 0.0F && hit.nativeShare > 0.0F) {
            hit.caster.heal(ownLoss * hit.nativeShare);
        }
    }

    private static final class Hit {
        final LivingEntity target;
        final DamageSource source;
        final float health;
        final float absorption;
        LivingEntity caster;
        float nativeShare;
        float childHealthLoss;
        float childAbsorptionLoss;

        Hit(LivingEntity target, DamageSource source) {
            this.target = target;
            this.source = source;
            this.health = Math.max(0.0F, target.getHealth());
            this.absorption = Math.max(0.0F, target.getAbsorptionAmount());
        }
    }
}
