package com.justenoughmods.achievementguide.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/** Caster identity installed only on the actual circle accepted by native spell spawning. */
public final class HealingCircleProof {
    private static final String KEY = "JemQuestHealingCircleCasterV4";
    private HealingCircleProof() {}
    public static void spawned(Entity circle, ServerPlayer actor) {
        if (circle instanceof Projectile projectile && projectile.getOwner() == actor && !circle.isRemoved()
                && circle.level() == actor.level() && actor.serverLevel().getEntity(circle.getUUID()) == circle
                && !actor.getAbilities().instabuild && !actor.isSpectator() && !(actor instanceof net.minecraftforge.common.util.FakePlayer)) {
            circle.getPersistentData().putUUID(KEY, actor.getUUID());
        }
    }
    public static ServerPlayer actor(Entity circle) {
        var tag = circle.getPersistentData();
        return circle instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer actor
                && tag.hasUUID(KEY) && actor.getUUID().equals(tag.getUUID(KEY)) && circle.level() == actor.level()
                && !circle.isRemoved() && actor.serverLevel().getEntity(circle.getUUID()) == circle ? actor : null;
    }
}
