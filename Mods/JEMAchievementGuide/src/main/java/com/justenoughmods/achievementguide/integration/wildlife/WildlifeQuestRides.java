package com.justenoughmods.achievementguide.integration.wildlife;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class WildlifeQuestRides {
    public interface NativeRide {
        String jem$wildlifeRideAction(ServerPlayer actor);
        default boolean jem$wildlifeRideFinished(ServerPlayer actor) { return true; }
    }
    public record Move(ServerPlayer actor, Entity vehicle, Vec3 before, String action) {}
    private WildlifeQuestRides() {}
    public static Move begin(ServerPlayer actor) {
        Entity vehicle = actor.getRootVehicle();
        if (WildlifeQuestProof.real(actor) == null || vehicle == actor || vehicle.getControllingPassenger() != actor
                || actor.zza == 0 && actor.xxa == 0 || !(vehicle instanceof NativeRide nativeRide)) return null;
        String action = nativeRide.jem$wildlifeRideAction(actor);
        return action.isEmpty() ? null : new Move(actor, vehicle, vehicle.position(), action);
    }
    public static void accepted(Move move) {
        if (move == null || move.actor().getRootVehicle() != move.vehicle() || move.vehicle().getControllingPassenger() != move.actor()
                || !(move.vehicle() instanceof NativeRide nativeRide) || !move.action().equals(nativeRide.jem$wildlifeRideAction(move.actor()))) return;
        double distance = move.before().distanceTo(move.vehicle().position()); if (!Double.isFinite(distance) || distance <= 0 || distance > 10) return;
        var proof = WildlifeQuestProof.data(move.vehicle());
        if (!WildlifeQuestProof.matches(move.vehicle(), "ride_actor", move.actor().getUUID()) || !proof.getString("ride_action").equals(move.action())) {
            WildlifeQuestProof.mark(move.vehicle(), "ride_actor", move.actor().getUUID()); proof.putString("ride_action", move.action()); proof.putDouble("ride_distance", 0);
        }
        double traveled = Math.min(5, proof.getDouble("ride_distance") + distance); proof.putDouble("ride_distance", traveled);
        if (traveled >= 5 && nativeRide.jem$wildlifeRideFinished(move.actor())) WildlifeQuestProof.emit(move.actor(), move.action());
    }
}
