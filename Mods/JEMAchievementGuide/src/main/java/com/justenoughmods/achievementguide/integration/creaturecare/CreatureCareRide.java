package com.justenoughmods.achievementguide.integration.creaturecare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;

public final class CreatureCareRide {
    public interface Saddle { boolean jem$careSaddled(); }
    public record Move(ServerPlayer actor, Entity vehicle, Vec3 before) {}
    private CreatureCareRide() {}
    public static Move begin(ServerPlayer player) {
        if (CreatureCareProof.real(player) == null) return null;
        Entity vehicle = player.getRootVehicle();
        if (vehicle == player || vehicle.getControllingPassenger() != player || (player.zza == 0 && player.xxa == 0)) return null;
        return new Move(player, vehicle, vehicle.position());
    }
    public static void accepted(Move move) {
        if (move == null || move.actor.getRootVehicle() != move.vehicle || move.vehicle.getControllingPassenger() != move.actor) return;
        double distance = move.before.distanceTo(move.vehicle.position());
        if (!Double.isFinite(distance) || distance <= 0 || distance > 10) return;
        String type = CreatureCareProof.type(move.vehicle);
        if (type.equals("primal:walrus") && move.vehicle instanceof AbstractHorse horse && horse.isTamed()
                && move.actor.getUUID().equals(horse.getOwnerUUID())) {
            CreatureCareProof.mark(horse, "walrus_ridden", move.actor.getUUID());
        }
    }
    public static void caribou(ServerPlayer actor, TamableAnimal animal, Vec3 before) {
        if (CreatureCareProof.real(actor) == null || before == null || animal.getFirstPassenger() != actor
                || animal.level() != actor.level() || (actor.zza == 0 && actor.xxa == 0)) return;
        double distance = Math.hypot(before.x - animal.getX(), before.z - animal.getZ());
        if (!Double.isFinite(distance) || distance <= 0 || distance > 10) return;
        if (CreatureCareProof.owned(animal, actor)
                && CreatureCareProof.matches(animal, "caribou_food_owner", actor.getUUID())
                && CreatureCareProof.matches(animal, "caribou_saddle_actor", actor.getUUID())
                && animal instanceof Saddle saddle && saddle.jem$careSaddled()) {
            var tag = CreatureCareProof.data(animal);
            if (!CreatureCareProof.matches(animal, "caribou_rider", actor.getUUID())) {
                CreatureCareProof.mark(animal, "caribou_rider", actor.getUUID()); tag.putDouble("caribou_travel", 0);
            }
            double travel = Math.min(5, tag.getDouble("caribou_travel") + distance); tag.putDouble("caribou_travel", travel);
            if (travel >= 5) CreatureCareProof.emit(actor, "v3_fauna_ringtail_and_caribou");
        }
    }
    public static ServerPlayer commanded(AbstractHorse walrus) {
        if (!(walrus.getControllingPassenger() instanceof ServerPlayer actor) || CreatureCareProof.real(actor) == null
                || !walrus.isTamed() || !actor.getUUID().equals(walrus.getOwnerUUID())
                || !CreatureCareProof.matches(walrus, "walrus_command", actor.getUUID())
                || !CreatureCareProof.matches(walrus, "walrus_ridden", actor.getUUID())
                || CreatureCareProof.data(walrus).getLong("walrus_command_until") < walrus.level().getGameTime()) return null;
        return actor;
    }
    public static void affected(AbstractHorse walrus) {
        ServerPlayer actor = commanded(walrus); if (actor != null) CreatureCareProof.emit(actor, "v3_fauna_walrus_signal");
    }
}
