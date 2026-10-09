package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import com.siirio.jemworldbosstiers.revival.ArenaService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;
import java.util.UUID;

public final class RaidArenaApi {
    public record Arena(String id, ResourceLocation entityType, ResourceLocation dimension,
                        BlockPos center, BoundingBox bounds) {
    }

    public static void register(MinecraftServer server, String id, ResourceLocation entityType,
                                ResourceLocation dimension, BoundingBox bounds) {
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (profile == null) return;
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord current = data.arena(id).orElse(null);
        BlockPos nativePosition=data.arenas().stream()
                .filter(arena->!arena.structureArena()&&arena.profileKey().equals(profile.key())&&arena.dimension().equals(dimension.toString()))
                .map(ArenaRecord::respawnPosition)
                .filter(bounds::isInside)
                .min(java.util.Comparator.comparingDouble(position->position.distSqr(new BlockPos(
                        bounds.minX()+bounds.getXSpan()/2,bounds.minY()+bounds.getYSpan()/2,bounds.minZ()+bounds.getZSpan()/2))))
                .orElse(null);
        if (current != null && current.profileKey().equals(profile.key()) && current.dimension().equals(dimension.toString())
                && sameBounds(current.bounds(), bounds)) {
            ArenaRecord registered=current.asStructureArena();
            if(nativePosition!=null) registered=registered.withRespawnPosition(nativePosition);
            if (!registered.unlocked() && data.defeatedBosses().contains(profile.key())) registered=registered.unlock();
            if(registered!=current) data.putArena(registered);
            return;
        }
        ArenaRecord exact = ArenaRecord.create(id, profile.key(), dimension.toString(), bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()).asStructureArena();
        if(nativePosition!=null) exact=exact.withRespawnPosition(nativePosition);
        if (data.defeatedBosses().contains(profile.key())) exact = exact.unlock();
        data.putArena(exact);
    }

    public static boolean compatible(MinecraftServer server, String arenaId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        return arena != null && arena.structureArena() && profile != null && arena.profileKey().equals(profile.key()) && arena.unlocked()
                && arena.activeEncounterId() == null && data.defeatedBosses().contains(profile.key());
    }

    public static boolean compatible(LivingEntity boss) {
        return arena(boss).isPresent();
    }

    public static Optional<Arena> arena(LivingEntity boss) {
        var profile=WorldTierApi.profile(boss).orElse(null);
        if(profile==null||boss.getServer()==null) return Optional.empty();
        WorldTierData data=WorldTierData.get(boss.getServer());
        String dimension=boss.level().dimension().location().toString();
        if(!data.defeatedBosses().contains(profile.key())) return Optional.empty();
        return data.arenas().stream()
                .filter(arena->arena.structureArena()&&arena.profileKey().equals(profile.key())&&arena.dimension().equals(dimension)&&arena.unlocked())
                .filter(arena->arena.activeEncounterId()==null||arena.activeEncounterId().equals(boss.getUUID()))
                .filter(arena->boss.getX()>=arena.minX()&&boss.getX()<arena.maxX()+1
                        &&boss.getZ()>=arena.minZ()&&boss.getZ()<arena.maxZ()+1)
                .min(java.util.Comparator.<ArenaRecord>comparingDouble(arena->arena.respawnPosition().distSqr(boss.blockPosition()))
                        .thenComparing(ArenaRecord::id))
                .map(arena->new Arena(arena.id(),BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()),
                        ResourceLocation.tryParse(arena.dimension()),arena.respawnPosition(),arena.bounds()));
    }

    public static Optional<BlockPos> respawnPosition(MinecraftServer server,String arenaId,ResourceLocation entityType) {
        var profile=BalanceRegistry.boss(entityType).orElse(null);
        return WorldTierData.get(server).arena(arenaId)
                .filter(ArenaRecord::structureArena)
                .filter(arena->profile!=null&&arena.profileKey().equals(profile.key()))
                .map(ArenaRecord::respawnPosition);
    }

    public static Optional<BlockPos> combatPosition(MinecraftServer server,String arenaId,ResourceLocation entityType) {
        var profile=BalanceRegistry.boss(entityType).orElse(null);
        ArenaRecord arena=WorldTierData.get(server).arena(arenaId)
                .filter(ArenaRecord::structureArena)
                .filter(value->profile!=null&&value.profileKey().equals(profile.key()))
                .orElse(null);
        if(arena==null) return Optional.empty();
        ResourceLocation dimension=ResourceLocation.tryParse(arena.dimension());
        ServerLevel level=dimension==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        return level!=null&&level.hasChunkAt(arena.respawnPosition())?Optional.of(arena.respawnPosition()):Optional.empty();
    }

    public static Optional<Arena> reserve(MinecraftServer server, String arenaId, ResourceLocation entityType, UUID reservationId) {
        if (!compatible(server, arenaId, entityType)) return Optional.empty();
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElseThrow();
        ArenaRecord reserved = arena.activate(reservationId, server.overworld().getGameTime()).orElse(null);
        if (reserved == null) return Optional.empty();
        data.putArena(reserved);
        return Optional.of(new Arena(reserved.id(), entityType, ResourceLocation.tryParse(reserved.dimension()),
                reserved.respawnPosition(), reserved.bounds()));
    }

    public static Optional<BlockPos> prepareBoss(MinecraftServer server, String arenaId, UUID reservationId,
                                                  LivingEntity boss) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = WorldTierApi.profile(boss).orElse(null);
        if (arena == null || profile == null || !arena.profileKey().equals(profile.key())
                || !reservationId.equals(arena.activeEncounterId())) {
            return Optional.empty();
        }
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) {
            return Optional.empty();
        }
        BlockPos respawnPosition = arena.respawnPosition();
        if (!level.hasChunkAt(respawnPosition)) {
            return Optional.empty();
        }
        ArenaRecord claimed = arena.release(reservationId)
                .activate(boss.getUUID(), level.getGameTime())
                .orElse(null);
        if (claimed == null) {
            return Optional.empty();
        }
        data.putArena(claimed);
        EncounterScaler.initialize(boss, profile, EncounterProvenance.RAID_EVENT, false, true, arenaId);
        return Optional.of(respawnPosition);
    }

    public static void release(MinecraftServer server, String arenaId, UUID encounterId) {
        WorldTierData data = WorldTierData.get(server);
        data.arena(arenaId).ifPresent(arena -> data.putArena(arena.release(encounterId)));
    }

    public static boolean reserveAgain(MinecraftServer server, String arenaId, UUID encounterId, UUID reservationId) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        if (arena == null) {
            return false;
        }
        ArenaRecord released = arena.release(encounterId);
        ArenaRecord reserved = released.activeEncounterId() == null
                ? released.activate(reservationId, server.overworld().getGameTime()).orElse(null)
                : null;
        if (reserved == null) {
            return false;
        }
        data.putArena(reserved);
        return true;
    }

    public static boolean sameBounds(BoundingBox first, BoundingBox second) {
        return first.minX() == second.minX() && first.minY() == second.minY() && first.minZ() == second.minZ()
                && first.maxX() == second.maxX() && first.maxY() == second.maxY() && first.maxZ() == second.maxZ();
    }

    private RaidArenaApi() {
    }
}
