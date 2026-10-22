package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native team tags select support; the canonical structure claim selects its encounter. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class HostedGroup {
    private static final String ARENA = "JEMHostedSupportArena";
    private static final String OWNER = "JEMHostedBoss";
    private static final Map<UUID, LivingEntity> LOADED = new HashMap<>();

    private HostedGroup() {}

    public static void detach(LivingEntity entity) {
        HostedLobby.release(entity);
        entity.getPersistentData().remove(ARENA);
        entity.getPersistentData().remove(OWNER);
        LOADED.remove(entity.getUUID());
    }

    public static boolean member(LivingEntity entity) { return entity.getPersistentData().contains(ARENA); }

    private static boolean support(BossProfile profile, LivingEntity entity) {
        return !WorldTierApi.profile(entity).map(BossProfile::hostedEncounter).orElse(false)
                && profile.allyTags().stream().anyMatch(id -> entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static LivingEntity owner(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return null;
        var tag = entity.getPersistentData();
        UUID id = member(entity) ? WorldTierData.get(level.getServer()).liveBoss(tag.getString(ARENA)).orElse(null)
                : tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        return id != null && level.getEntity(id) instanceof LivingEntity root ? root : null;
    }

    private static void enroll(LivingEntity entity, String arena) {
        entity.getPersistentData().putString(ARENA, arena);
        LOADED.put(entity.getUUID(), entity);
        synchronize(entity);
    }

    private static void synchronize(LivingEntity entity) {
        LivingEntity root = owner(entity);
        if (root != null) entity.getPersistentData().putUUID(OWNER, root.getUUID());
        if (root != null && HostedEncounters.isHosted(root)) HostedLobby.release(entity);
        else HostedLobby.hold(entity);
    }

    public static void stage(LivingEntity root) {
        if (!(root.level() instanceof ServerLevel level) || !HostedEncounterApi.enabled(level.getServer())) return;
        var profile = WorldTierApi.profile(root).orElse(null);
        var encounter = WorldTierApi.encounter(root).orElse(null);
        if (profile == null || profile.allyTags().isEmpty() || encounter == null || encounter.arenaId() == null) return;
        var data = WorldTierData.get(level.getServer());
        var arena = data.arena(encounter.arenaId()).filter(value -> value.structureArena()).orElse(null);
        if (arena == null || data.liveBoss(arena.id()).filter(root.getUUID()::equals).isEmpty()) return;
        var box = new AABB(arena.minX(), arena.minY(), arena.minZ(), arena.maxX() + 1, arena.maxY() + 1, arena.maxZ() + 1);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box, value -> value.isAlive() && support(profile, value)))
            enroll(entity, arena.id());
    }

    public static void release(LivingEntity root) {
        for (var entity : List.copyOf(LOADED.values()))
            if (!entity.isRemoved() && belongs(entity, root)) HostedLobby.release(entity);
    }

    public static void hold(LivingEntity root) {
        for (var entity : List.copyOf(LOADED.values()))
            if (!entity.isRemoved() && belongs(entity, root)) HostedLobby.hold(entity);
    }

    private static boolean belongs(LivingEntity entity, LivingEntity root) {
        return entity.level() == root.level() && entity.getPersistentData().hasUUID(OWNER)
                && entity.getPersistentData().getUUID(OWNER).equals(root.getUUID());
    }

    public static void finish(LivingEntity root) {
        for (var entity : List.copyOf(LOADED.values())) if (belongs(entity, root)) {
            HostedLobby.release(entity);
            entity.getPersistentData().remove(ARENA);
            entity.getPersistentData().remove(OWNER);
            LOADED.remove(entity.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity entity)) return;
        if (!HostedEncounterApi.enabled(level.getServer())) { detach(entity); return; }
        level.getServer().execute(() -> {
            if (entity.isRemoved()) return;
            if (member(entity)) { LOADED.put(entity.getUUID(), entity); synchronize(entity); return; }
            if (HostedEncounterApi.canHost(entity)) { stage(entity); return; }
            var data = WorldTierData.get(level.getServer());
            var matches = data.arenas().stream().filter(arena -> arena.structureArena()
                    && arena.contains(level.dimension().location().toString(), entity.getX(), entity.getY(), entity.getZ())
                    && BalanceRegistry.bossByKey(arena.profileKey()).filter(profile -> support(profile, entity)).isPresent()).toList();
            if (matches.size() == 1) enroll(entity, matches.get(0).id());
        });
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (!HostedEncounterApi.enabled(event.getServer()) || event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        for (var entity : List.copyOf(LOADED.values())) if (!entity.isRemoved()) synchronize(entity);
    }

    @SubscribeEvent public static void left(EntityLeaveLevelEvent event) { LOADED.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LOADED.clear(); }
}
