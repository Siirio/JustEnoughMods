package com.justenoughmods.achievementguide.integration.travel;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;

/** Admission and bounded observational context, never a movement or quest-progress owner. */
public final class TravelProof {
    private static Method identityAdmission;
    private TravelProof() {}

    public static boolean eligible(ServerPlayer actor) {
        return actor != null && !(actor instanceof FakePlayer) && actor.isAlive() && !actor.isSpectator()
                && !actor.getAbilities().instabuild && actor.server.isSameThread()
                && actor.server.getPlayerList().getPlayer(actor.getUUID()) == actor
                && actor.connection != null && actor.connection.isAcceptingMessages() && authenticated(actor);
    }

    public static boolean authenticated(ServerPlayer actor) {
        if (!ModList.get().isLoaded("jem_identity")) return !actor.server.isDedicatedServer() || actor.server.usesAuthentication();
        try {
            if (identityAdmission == null) identityAdmission = Class.forName("com.siirio.jemidentity.api.IdentityPolicy")
                    .getMethod("permitsAccountMutation", ServerPlayer.class);
            return Boolean.TRUE.equals(identityAdmission.invoke(null, actor));
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("Travel identity owner failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Travel identity owner API changed", failure);
        }
    }

    public static ServerPlayer controller(Entity vehicle) {
        return !vehicle.isRemoved() && vehicle.getControllingPassenger() instanceof ServerPlayer actor && eligible(actor)
                && actor.getRootVehicle() == vehicle && actor.level() == vehicle.level() ? actor : null;
    }

    public static void emit(ServerPlayer actor, String action) {
        if (eligible(actor)) QuestActionTrigger.INSTANCE.emit(actor, action);
    }

    public static boolean finite(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    public static final class Journey {
        public final ServerPlayer actor;
        public final Vec3 start;
        public final int began;
        public Vec3 last;
        public boolean fuelConsumed;

        public Journey(ServerPlayer actor, Vec3 position, Entity vehicle) {
            this.actor = actor;
            start = position;
            last = position;
            began = vehicle.tickCount;
        }

        public boolean valid(Entity vehicle, ServerPlayer pilot, Vec3 from) {
            return actor == pilot && controller(vehicle) == actor && actor.level() == vehicle.level()
                    && vehicle.tickCount >= began && vehicle.tickCount - began <= 24000
                    && finite(from) && finite(vehicle.position()) && last.distanceToSqr(from) <= 1.0E-8;
        }

        public boolean reached(Entity vehicle) {
            double x = vehicle.getX() - start.x;
            double z = vehicle.getZ() - start.z;
            return x * x + z * z >= 64.0 * 64.0;
        }
    }
}
