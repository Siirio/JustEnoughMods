package com.siirio.jemworldbosstiers.balance;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-thread hurt-call transactions; no persistent or client authority. */
public final class GuardHitTracker {
    private final Deque<Hit> hits = new ArrayDeque<>();

    public void begin(Object wearer, float health, float absorption) {
        hits.push(new Hit(wearer, health, absorption));
    }

    public boolean reserve(Object wearer) {
        Hit current = hits.peek();
        if (current == null || current.wearer != wearer) return false;
        for (Hit hit : hits) {
            if (hit.wearer == wearer && hit.guarded) return false;
        }
        current.guarded = true;
        return true;
    }

    public boolean finish(Object wearer, float health, float absorption) {
        Hit current = hits.pop();
        if (current.wearer != wearer) throw new IllegalStateException("Unbalanced guard hurt boundary");
        float healthLoss = Math.max(0, current.health - health);
        float absorptionLoss = Math.max(0, current.absorption - absorption);
        boolean accepted = healthLoss > current.childHealthLoss
                || absorptionLoss > current.childAbsorptionLoss;
        for (Hit parent : hits) {
            if (parent.wearer == wearer) {
                parent.childHealthLoss += healthLoss;
                parent.childAbsorptionLoss += absorptionLoss;
                break;
            }
        }
        return current.guarded && accepted;
    }

    public boolean isEmpty() {
        return hits.isEmpty();
    }

    private static final class Hit {
        final Object wearer;
        final float health;
        final float absorption;
        float childHealthLoss;
        float childAbsorptionLoss;
        boolean guarded;

        Hit(Object wearer, float health, float absorption) {
            this.wearer = wearer;
            this.health = health;
            this.absorption = absorption;
        }
    }
}
