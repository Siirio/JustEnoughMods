package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit sandbox-only replacement: three authored cuts and forward pulls. */
@Mod.EventBusSubscriber(modid = "jem_visual_sandbox")
public final class FierySlash {
    private static final Map<UUID, Combo> ACTIVE = new HashMap<>();
    private static final class Combo {
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        final Vec3 direction;
        final long start;
        final int stage;
        Combo(ServerPlayer player, Vec3 direction) {
            dimension = player.serverLevel().dimension();
            stage = RaceExperience.tier(player);
            this.direction = direction;
            start = player.level().getGameTime();
        }
    }
    private FierySlash() {}

    public static boolean cast(ServerPlayer player) {
        if (!allowed(player) || player.isPassenger() || ACTIVE.containsKey(player.getUUID())) return false;
        Vec3 forward = player.getLookAngle().multiply(1, 0, 1);
        // Straight up/down aiming retains the player's horizontal facing.
        if (forward.lengthSqr() < 0.0001) {
            double yaw = Math.toRadians(player.getYRot());
            forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        Combo combo = new Combo(player, forward.normalize());
        ACTIVE.put(player.getUUID(), combo);
        beginHit(player, combo, 0);
        return true;
    }

    private static boolean allowed(ServerPlayer player) {
        return RaceExperience.canAct(player)&&RaceExperience.race(player)==4;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || !RaceExperience.serverActive(server)) return;
        ACTIVE.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Combo combo = entry.getValue();
            if (player == null || !allowed(player) || player.serverLevel().dimension() != combo.dimension || player.isPassenger()) return true;
            long age = player.level().getGameTime() - combo.start;
            if (age >= 38 || age < 0) return true;
            int hit = age < 10 ? 0 : age < 22 ? 1 : 2;
            int phase = (int) age - new int[]{0,10,22}[hit];
            if (age == 10 || age == 22) beginHit(player, combo, hit);
            int impact = hit == 2 ? 5 : 4;
            if (phase < impact) pull(player, combo.direction);
            if (phase == impact) strike(player, combo.direction, combo.stage);
            return false;
        });
    }

    private static void beginHit(ServerPlayer player, Combo combo, int hit) {
        SandboxNetwork.slash(player, hit, player.position().add(combo.direction.scale(1.35)),
                (float) Math.toDegrees(Math.atan2(-combo.direction.x, combo.direction.z)));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private static void pull(ServerPlayer player, Vec3 forward) {
        Vec3 step = forward.scale(0.42);
        var level = player.serverLevel();
        var box = player.getBoundingBox();
        // Full swept body clearance and support: no wall clipping or ledge dash.
        boolean clear = level.noCollision(player, box.expandTowards(step))
                && level.getWorldBorder().isWithinBounds(box.move(step))
                && !level.noCollision(player, box.move(step).move(0, -0.55, 0));
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(clear ? new Vec3(step.x, velocity.y, step.z) : new Vec3(0, velocity.y, 0));
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void strike(ServerPlayer player, Vec3 forward, int stage) {
        Vec3 center = player.position().add(forward.scale(1.65)).add(0, 0.9, 0);
        var area = new net.minecraft.world.phys.AABB(center, center).inflate(1.8, 1.5, 1.8);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())) {
            Vec3 offset = target.position().subtract(player.position());
            if (offset.dot(forward) < 0 || !player.hasLineOfSight(target)) continue;
            // Snapshot the accepted combo stage across all three impacts.
            target.hurt(player.damageSources().onFire(), RacePowerStages.slashDamage(stage));
            target.setSecondsOnFire(RacePowerStages.fireSeconds(stage));
            target.push(forward.x * 0.18, 0.12, forward.z * 0.18);
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0, velocity.y, 0);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    static void selectionChanged(ServerPlayer p){if(ACTIVE.remove(p.getUUID())!=null){p.setDeltaMovement(0,p.getDeltaMovement().y,0);p.connection.send(new ClientboundSetEntityMotionPacket(p));}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }
}
