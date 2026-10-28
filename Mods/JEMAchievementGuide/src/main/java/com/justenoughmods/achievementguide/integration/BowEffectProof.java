package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/** One synchronous native projectile effect call, never a second effect registry */
public final class BowEffectProof {
    private static final ThreadLocal<ArrayDeque<Projectile>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    private BowEffectProof() {}

    public static void begin(Projectile source) { CALLS.get().push(source); }
    public static void end() {
        var calls = CALLS.get();
        if (!calls.isEmpty()) calls.pop();
        if (calls.isEmpty()) CALLS.remove();
    }

    public static void committed(Entity owner, String bow, String action) {
        ServerPlayer actor = actor(owner, bow);
        if (actor != null) QuestActionTrigger.INSTANCE.emit(actor, action);
    }

    public static ServerPlayer actor(Entity owner, String bow) {
        var calls = CALLS.get();
        if (calls.isEmpty()) { CALLS.remove(); return null; }
        Projectile source = calls.peek();
        var proof = source.getPersistentData();
        if (owner instanceof ServerPlayer actor && source.getOwner() == owner
                && proof.hasUUID("JemQuestNativeBowActorV4")
                && actor.getUUID().equals(proof.getUUID("JemQuestNativeBowActorV4"))
                && bow.equals(proof.getString("JemQuestNativeBowSubjectV4"))) {
            return actor;
        }
        return null;
    }
}
