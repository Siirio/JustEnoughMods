package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared rift impact/echo algorithms used by the authored final power timelines. */
public final class SandboxSkills {
    private SandboxSkills() {}
    public static LivingEntity riftTarget(ServerPlayer player) {
        return target(player, RacePowerStages.riftRange(RaceExperience.tier(player)));
    }
    public static boolean commitRift(ServerPlayer player, UUID id) {
        var entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || player.distanceToSqr(target) > Math.pow(RacePowerStages.riftRange(RaceExperience.tier(player)), 2)) {
            fail(player,"target_required"); return false;
        }
        Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.8));
        BlockPos safe = com.siirio.jemroles.trait.RaceEnvironment.nearestSafe(player, player.serverLevel(), BlockPos.containing(behind), 3, true).orElse(null);
        if (safe == null) { fail(player,"no_safe_destination"); return false; }
        return riftAmbush(player,RaceExperience.tier(player),target,safe);
    }
    private static boolean riftAmbush(ServerPlayer player, int tier, LivingEntity target, BlockPos safe) {
        Vec3 arrival = Vec3.atBottomCenterOf(safe);
        if (!teleport(player, arrival, target.getYRot())) return false;
        SandboxHostileControl.refreshTargets(player);
        player.attack(target);
        if (RacePowerStages.riftSlowTicks(tier) > 0) for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(RacePowerStages.riftSlowRadius(tier)), e -> e != player && e != target))
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RacePowerStages.riftSlowTicks(tier), 2));
        return true;
    }

    public static void echoVision(ServerPlayer player) {
        List<Integer> revealed = new ArrayList<>();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RacePowerStages.echoRadius(RaceExperience.tier(player))), entity -> entity != player && entity.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            if (revealed.size() < 8) revealed.add(target.getId());
        }
        if (revealed.isEmpty()) { fail(player, "no_nearby_entities"); return; }
        SandboxNetwork.effect(player,24,3,player.position(),0,player.getId());
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition(), look = player.getViewVector(1.0F), to = from.add(look.scale(range));
        HitResult obstruction = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS) to = obstruction.getLocation();
        Vec3 end = to;
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()).stream()
                .filter(e -> e.getBoundingBox().inflate(0.5D).clip(from, end).isPresent())
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static void fail(ServerPlayer player, String reason) {
        player.sendSystemMessage(Component.translatable("jem.roles.ability." + reason), true);
    }

    private static boolean teleport(ServerPlayer player, Vec3 destination, float yaw) {
        // This overload returns the result of the existing hosted-arena teleport gate.
        // A gate may redirect instead of deny, so only the actual requested arrival commits the ability.
        boolean moved = player.teleportTo(player.serverLevel(), destination.x, destination.y, destination.z,
                Set.of(), yaw, player.getXRot());
        if (!moved || player.position().distanceToSqr(destination) > 0.0001D) {
            fail(player, "teleport_blocked");
            return false;
        }
        return true;
    }
}
