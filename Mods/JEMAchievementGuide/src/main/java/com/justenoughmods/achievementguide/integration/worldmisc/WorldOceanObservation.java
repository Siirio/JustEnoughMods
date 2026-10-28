package com.justenoughmods.achievementguide.integration.worldmisc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded gaze observation driven by accepted native player packets, no world poll. */
public final class WorldOceanObservation {
    private record Sight(ServerPlayer actor, Entity target, long began, long last) {}
    private static final Map<UUID, Sight> SIGHTS = new HashMap<>();
    private WorldOceanObservation() {}
    public static void accepted(ServerPlayer actor) {
        if (!WorldNativeQuestProof.real(actor)) { clear(actor); return; }
        Vec3 eye = actor.getEyePosition(), end = eye.add(actor.getLookAngle().scale(16));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(actor, eye, end,
                actor.getBoundingBox().expandTowards(actor.getLookAngle().scale(16)).inflate(1),
                target -> WorldNativeQuestProof.entity(target, "oceanicrealms:great_white_shark")
                        || WorldNativeQuestProof.entity(target, "oceanicrealms:whaleshark"), 256);
        Entity target = hit == null ? null : hit.getEntity();
        if (target == null || !WorldNativeQuestProof.nativeMob(target) || !target.isAlive() || !actor.hasLineOfSight(target)
                || !actor.level().getBiome(target.blockPosition()).is(BiomeTags.IS_OCEAN)) { clear(actor); return; }
        long now = actor.level().getGameTime();
        Sight old = SIGHTS.get(actor.getUUID());
        Sight sight = old != null && old.actor == actor && old.target == target && now >= old.last && now - old.last <= 20
                ? new Sight(actor, target, old.began, now) : new Sight(actor, target, now, now);
        SIGHTS.put(actor.getUUID(), sight);
        if (now - sight.began >= 40) { clear(actor); WorldNativeQuestProof.emit(actor, "v3_fauna_ocean_watch"); }
    }
    public static void damaged(Entity target, Entity actor) {
        if (actor instanceof ServerPlayer player) {
            Sight sight = SIGHTS.get(player.getUUID());
            if (sight != null && sight.target == target) clear(player);
        }
    }
    public static void clear(ServerPlayer actor) { SIGHTS.remove(actor.getUUID()); }
    public static void clear() { SIGHTS.clear(); }
}
