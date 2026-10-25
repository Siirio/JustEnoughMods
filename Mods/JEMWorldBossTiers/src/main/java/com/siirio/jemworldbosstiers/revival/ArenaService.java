package com.siirio.jemworldbosstiers.revival;

import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;

public final class ArenaService {
    private static final int SAFE_HORIZONTAL_RADIUS = 4;
    private static final int SAFE_VERTICAL_RADIUS = 64;

    private ArenaService() {
    }

    /** Block availability alone does not prove that saved chunk entities were read. */
    public static boolean entitiesLoaded(ServerLevel level,ArenaRecord arena) {
        if(!arena.dimension().equals(level.dimension().location().toString())
                ||!(level instanceof com.siirio.jemworldbosstiers.mixin.ServerLevelEntityManagerAccessor access)) return false;
        var manager=access.jemWorldBossTiers$entityManager();
        for(int x=arena.minX()>>4;x<=arena.maxX()>>4;x++) {
            for(int z=arena.minZ()>>4;z<=arena.maxZ()>>4;z++) {
                if(!level.hasChunkAt(new BlockPos(x<<4,arena.respawnPosition().getY(),z<<4))
                        ||!manager.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(x,z))) return false;
            }
        }
        return true;
    }

    public static ArenaRecord findOrRegister(LivingEntity entity, BossProfile profile) {
        if (!profile.ownsExclusiveArena()) throw new IllegalArgumentException("Profile does not own an exclusive arena: " + profile.key());
        ServerLevel level = (ServerLevel) entity.level();
        WorldTierData data = WorldTierData.get(level.getServer());
        String dimension = level.dimension().location().toString();
        var encounter = com.siirio.jemworldbosstiers.api.WorldTierApi.encounter(entity).orElse(null);
        ArenaRecord bound = encounter == null || encounter.arenaId() == null ? null : data.arena(encounter.arenaId()).orElse(null);
        if (bound != null && bound.profileKey().equals(profile.key()) && bound.dimension().equals(dimension)) return bound;
        Optional<ArenaRecord> existing = data.arenas().stream()
                .filter(arena -> arena.profileKey().equals(profile.key()) && arena.contains(dimension, entity.getX(), entity.getY(), entity.getZ()))
                .max(java.util.Comparator.comparing(ArenaRecord::structureArena));
        if (existing.isPresent()) {
            ArenaRecord current=existing.get();
            ArenaRecord arena=current.structureArena()?current:current.withRespawnPosition(entity.blockPosition());
            data.putArena(arena);
            return arena;
        }
        int radius = profile.arenaRadius();
        int centerX = entity.blockPosition().getX();
        int centerZ = entity.blockPosition().getZ();
        String id = dimension + '|' + profile.key() + '|' + centerX + '|' + centerZ;
        ArenaRecord arena = ArenaRecord.create(id, profile.key(), dimension, centerX - radius, level.getMinBuildHeight(), centerZ - radius, centerX + radius, level.getMaxBuildHeight() - 1, centerZ + radius);
        ArenaRecord positioned=arena.withRespawnPosition(entity.blockPosition());
        data.putArena(positioned);
        return positioned;
    }

    public static void unlockAndRelease(WorldTierData data, String arenaId, java.util.UUID encounterId) {
        if (data.liveBoss(arenaId).filter(id -> !id.equals(encounterId)).isPresent()) {
            data.clearLiveBoss(arenaId, encounterId);
            return;
        }
        data.clearLiveBoss(arenaId, encounterId);
        data.arena(arenaId).ifPresent(arena -> data.putArena(arena.unlock().release(encounterId)));
    }

    public static void reconcileStructure(WorldTierData data, ArenaRecord structure) {
        if (!structure.structureArena()) return;
        for (ArenaRecord previous : data.arenas()) if (!previous.structureArena()) data.aliasArena(previous.id(), structure.id());
    }

    public static Optional<BlockPos> safePosition(ServerLevel level, BlockPos preferred, BoundingBox bounds) {
        Optional<BlockPos> sheltered=findSafePosition(level,preferred,bounds,true);
        return sheltered.isPresent()?sheltered:findSafePosition(level,preferred,bounds,false);
    }

    private static Optional<BlockPos> findSafePosition(ServerLevel level,BlockPos preferred,BoundingBox bounds,boolean sheltered) {
        int minY=Math.max(level.getMinBuildHeight()+1,bounds.minY()+1);
        int maxY=Math.min(level.getMaxBuildHeight()-2,bounds.maxY());
        int centerY=Math.max(minY,Math.min(maxY,preferred.getY()));
        int verticalRadius=Math.min(SAFE_VERTICAL_RADIUS,maxY-minY);
        for (int vertical = 0; vertical <= verticalRadius; vertical++) {
            for (int direction : vertical == 0 ? new int[]{0} : new int[]{vertical, -vertical}) {
                int y = centerY + direction;
                if (y < minY || y > maxY) {
                    continue;
                }
                for (int radius = 0; radius <= SAFE_HORIZONTAL_RADIUS; radius++) {
                    for (int x = Math.max(bounds.minX(),preferred.getX() - radius); x <= Math.min(bounds.maxX(),preferred.getX() + radius); x++) {
                        for (int z = Math.max(bounds.minZ(),preferred.getZ() - radius); z <= Math.min(bounds.maxZ(),preferred.getZ() + radius); z++) {
                            if (radius > 0 && x != preferred.getX() - radius && x != preferred.getX() + radius
                                    && z != preferred.getZ() - radius && z != preferred.getZ() + radius) {
                                continue;
                            }
                            BlockPos candidate = new BlockPos(x, y, z);
                            if (level.hasChunkAt(candidate)
                                    && (!sheltered||!level.canSeeSky(candidate))
                                    && level.getBlockState(candidate.below()).isFaceSturdy(level,candidate.below(),net.minecraft.core.Direction.UP)
                                    && !level.getBlockState(candidate.below()).is(Blocks.CACTUS)
                                    && !level.getBlockState(candidate.below()).is(Blocks.FIRE)
                                    && !level.getBlockState(candidate.below()).is(Blocks.SOUL_FIRE)
                                    && !level.getBlockState(candidate.below()).is(Blocks.MAGMA_BLOCK)
                                    && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                                    && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()
                                    && level.getFluidState(candidate).isEmpty()
                                    && level.getFluidState(candidate.above()).isEmpty()) {
                                return Optional.of(candidate);
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
