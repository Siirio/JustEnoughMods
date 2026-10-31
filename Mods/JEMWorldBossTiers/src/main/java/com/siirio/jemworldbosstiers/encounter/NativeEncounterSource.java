package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Exact synchronous native tick provenance, never proximity or participant discovery. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class NativeEncounterSource {
    private static final String OWNER = "JEMHostedNativeOwner";
    private static final String DIMENSION = "JEMHostedNativeDimension";
    private static final String ATTEMPT = "JEMHostedNativeAttempt";
    private static final ThreadLocal<Entity> CURRENT = new ThreadLocal<>();

    private NativeEncounterSource() {}

    public static void run(Entity ticking, Runnable tick) {
        if (ticking.getServer() == null || !HostedEncounterApi.enabled(ticking.getServer())) {
            tick.run();
            return;
        }
        Entity previous = CURRENT.get();
        Entity owner = canonicalOwner(ticking);
        if (denied(ticking)) owner = ticking;
        if (owner == null) CURRENT.remove();
        else CURRENT.set(owner);
        try { tick.run(); }
        finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    /** Canonical owner, or an exact marked source without valid current-attempt permission. */
    public static Entity current() { return CURRENT.get(); }

    public static boolean unresolved(Entity entity) {
        if (entity == null || entity instanceof Player || entity instanceof ItemEntity || entity instanceof ExperienceOrb
                || !entity.getPersistentData().hasUUID(OWNER)) return false;
        // An explicit native player or boss owner always outranks an obsolete marker.
        return HostedEncounterApi.damageOwner(entity) == entity;
    }

    /** Marked old effects retain their native lifetime, but never inherit a newer attempt. */
    public static boolean denied(Entity entity) {
        if (entity == null || entity instanceof Player || entity instanceof ItemEntity || entity instanceof ExperienceOrb
                || !entity.getPersistentData().hasUUID(OWNER)) return false;
        if (entity.getServer() != null && !HostedEncounterApi.enabled(entity.getServer())) return false;
        Entity nativeOwner = HostedEncounterApi.damageOwner(entity);
        if (nativeOwner instanceof Player) return false;
        Entity canonical = canonicalOwner(entity);
        var marker = entity.getPersistentData();
        if (!(canonical instanceof LivingEntity boss) || !canonical.getUUID().equals(marker.getUUID(OWNER)) || !marker.hasUUID(ATTEMPT)) return true;
        return !marker.getUUID(ATTEMPT).equals(HostedEncounters.attemptId(boss));
    }

    /** Native ownership is resolved by the caller before this persisted fallback is consulted. */
    public static Entity owner(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.getPersistentData().hasUUID(OWNER)) return null;
        var marker = entity.getPersistentData();
        if (marker.contains(DIMENSION)) {
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(marker.getString(DIMENSION));
            if (dimension == null || level.getServer() == null) return null;
            level = level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
            if (level == null) return null;
        }
        Entity owner = level.getEntity(marker.getUUID(OWNER));
        return owner instanceof LivingEntity && !(owner instanceof Player) && owner != entity ? owner : null;
    }

    private static Entity canonicalOwner(Entity entity) {
        if (entity instanceof Player || entity instanceof ItemEntity || entity instanceof ExperienceOrb) return null;
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (!(owner instanceof LivingEntity living) || owner instanceof Player) return null;
        if (HostedEncounters.isHosted(living)) return living;
        LivingEntity group = HostedGroup.owner(living);
        if (group != null && (HostedEncounters.isHosted(group) || HostedLobby.held(group))) return group;
        return HostedLobby.held(living) ? living : null;
    }

    static boolean capture(Entity entity) {
        Entity source = current();
        if (!(source instanceof LivingEntity boss) || !HostedEncounters.isHosted(boss) || HostedLobby.held(boss)
                || entity == source || !(source.level() instanceof ServerLevel sourceLevel)
                || !(entity.level() instanceof ServerLevel targetLevel) || sourceLevel.getServer() != targetLevel.getServer()
                || entity instanceof Player || entity instanceof ItemEntity || entity instanceof ExperienceOrb) return false;
        var marker = entity.getPersistentData();
        if (marker.hasUUID(OWNER) && !marker.getUUID(OWNER).equals(source.getUUID())) return false;
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (entity instanceof net.minecraft.world.entity.OwnableEntity owned && owned.getOwnerUUID() != null && owned.getOwner() == null) return false;
        if (owner instanceof Player || owner != entity && owner != source && canonicalOwner(owner) != source) return false;
        var attempt = HostedEncounters.attemptId(boss);
        if (attempt == null) return false;
        if (marker.hasUUID(OWNER)) return marker.hasUUID(ATTEMPT) && marker.getUUID(ATTEMPT).equals(attempt);
        marker.putUUID(OWNER, source.getUUID());
        marker.putString(DIMENSION, source.level().dimension().location().toString());
        marker.putUUID(ATTEMPT, attempt);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && !event.loadedFromDisk()) capture(event.getEntity());
    }
}
