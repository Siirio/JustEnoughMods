package com.justenoughmods.achievementguide.integration.travel;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One native catch episode per online actor, discarded with the native lifecycle. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class GougeTravelProof {
    private static final Map<UUID, Catch> catches = new HashMap<>();
    private static final class Catch {
        final ServerPlayer actor;
        final Level level;
        final int began;
        boolean supported;
        Catch(ServerPlayer actor) { this.actor = actor; level = actor.level(); began = actor.tickCount; }
    }
    private GougeTravelProof() {}

    public static void impact(ServerPlayer actor, boolean toolSurvived, double downwardSpeed) {
        reset(actor.getUUID());
        if (TravelProof.eligible(actor) && toolSurvived && Double.isFinite(downwardSpeed) && downwardSpeed > 0
                && actor.getDeltaMovement().y < 0 && actor.fallDistance > 0 && !actor.onGround()) {
            catches.put(actor.getUUID(), new Catch(actor));
        }
    }

    private static Catch current(ServerPlayer actor) {
        Catch receipt = catches.get(actor.getUUID());
        if (receipt != null && (receipt.actor != actor || receipt.level != actor.level() || !TravelProof.eligible(actor)
                || actor.onGround() || actor.tickCount - receipt.began > 24000 || actor.tickCount < receipt.began)) {
            catches.remove(actor.getUUID());
            return null;
        }
        return receipt;
    }

    public static void supported(ServerPlayer actor, boolean nativeSuccess, boolean nativeActive) {
        Catch receipt = current(actor);
        if (!nativeSuccess || !nativeActive) reset(actor.getUUID());
        else if (receipt != null) receipt.supported = true;
    }

    public static void kicked(ServerPlayer actor, boolean nativeSuccess) {
        Catch receipt = current(actor);
        reset(actor.getUUID());
        if (receipt != null && receipt.supported && nativeSuccess) TravelProof.emit(actor, "v3_world_wall_catch");
    }

    public static void reset(UUID actor) { catches.remove(actor); }
    private static void reset(Entity actor) { if (actor instanceof ServerPlayer) reset(actor.getUUID()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { reset(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { reset(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { reset(event.getOriginal()); reset(event.getEntity()); }
    @SubscribeEvent public static void died(LivingDeathEvent event) { reset(event.getEntity()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { catches.clear(); }
}
