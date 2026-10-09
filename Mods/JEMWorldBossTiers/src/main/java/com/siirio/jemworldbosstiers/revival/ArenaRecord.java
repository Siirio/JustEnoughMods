package com.siirio.jemworldbosstiers.revival;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;
import java.util.UUID;

public record ArenaRecord(
        String id,
        ResourceLocation profileKey,
        String dimension,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        BlockPos respawnPosition,
        boolean structureArena,
        boolean unlocked,
        UUID activeEncounterId,
        long activeSince
) {
    public static ArenaRecord create(String id, ResourceLocation profileKey, String dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int normalizedMinX=Math.min(minX,maxX),normalizedMinY=Math.min(minY,maxY),normalizedMinZ=Math.min(minZ,maxZ);
        int normalizedMaxX=Math.max(minX,maxX),normalizedMaxY=Math.max(minY,maxY),normalizedMaxZ=Math.max(minZ,maxZ);
        return new ArenaRecord(id,profileKey,dimension,normalizedMinX,normalizedMinY,normalizedMinZ,normalizedMaxX,normalizedMaxY,normalizedMaxZ,
                new BlockPos(normalizedMinX+(normalizedMaxX-normalizedMinX)/2,normalizedMinY+(normalizedMaxY-normalizedMinY)/2,normalizedMinZ+(normalizedMaxZ-normalizedMinZ)/2),false,false,null,0L);
    }

    public ArenaRecord withRespawnPosition(BlockPos position) {
        BlockPos immutable=position.immutable();
        return immutable.equals(respawnPosition) ? this : new ArenaRecord(id,profileKey,dimension,minX,minY,minZ,maxX,maxY,maxZ,immutable,structureArena,unlocked,activeEncounterId,activeSince);
    }

    public ArenaRecord asStructureArena() {
        return structureArena ? this : new ArenaRecord(id,profileKey,dimension,minX,minY,minZ,maxX,maxY,maxZ,respawnPosition,true,unlocked,activeEncounterId,activeSince);
    }

    public ArenaRecord unlock() {
        return unlocked ? this : new ArenaRecord(id, profileKey, dimension, minX, minY, minZ, maxX, maxY, maxZ, respawnPosition, structureArena, true, activeEncounterId, activeSince);
    }

    public Optional<ArenaRecord> activate(UUID encounterId, long gameTime) {
        if (!unlocked || activeEncounterId != null) {
            return Optional.empty();
        }
        return Optional.of(new ArenaRecord(id, profileKey, dimension, minX, minY, minZ, maxX, maxY, maxZ, respawnPosition, structureArena, true, encounterId, gameTime));
    }

    public ArenaRecord recover(UUID encounterId, long gameTime) {
        return new ArenaRecord(id, profileKey, dimension, minX, minY, minZ, maxX, maxY, maxZ, respawnPosition, structureArena, unlocked, encounterId, gameTime);
    }

    public ArenaRecord release(UUID encounterId) {
        if (activeEncounterId == null || !activeEncounterId.equals(encounterId)) {
            return this;
        }
        return new ArenaRecord(id, profileKey, dimension, minX, minY, minZ, maxX, maxY, maxZ, respawnPosition, structureArena, unlocked, null, 0L);
    }

    public ArenaRecord releaseIfStale(long gameTime, long timeout) {
        if (activeEncounterId == null || gameTime - activeSince < timeout) {
            return this;
        }
        return new ArenaRecord(id, profileKey, dimension, minX, minY, minZ, maxX, maxY, maxZ, respawnPosition, structureArena, unlocked, null, 0L);
    }

    public boolean contains(String dimension, double x, double y, double z) {
        return this.dimension.equals(dimension) && x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public BlockPos center() {
        return new BlockPos(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2, minZ + (maxZ - minZ) / 2);
    }

    public BoundingBox bounds() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Profile", profileKey.toString());
        tag.putString("Dimension", dimension);
        tag.putInt("MinX", minX);
        tag.putInt("MinY", minY);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxY", maxY);
        tag.putInt("MaxZ", maxZ);
        tag.putInt("RespawnX",respawnPosition.getX());
        tag.putInt("RespawnY",respawnPosition.getY());
        tag.putInt("RespawnZ",respawnPosition.getZ());
        tag.putBoolean("StructureArena",structureArena);
        tag.putBoolean("Unlocked", unlocked);
        if (activeEncounterId != null) {
            tag.putUUID("ActiveEncounter", activeEncounterId);
            tag.putLong("ActiveSince", activeSince);
        }
        return tag;
    }

    public static Optional<ArenaRecord> load(CompoundTag tag) {
        ResourceLocation profile = ResourceLocation.tryParse(tag.getString("Profile"));
        if (profile == null || tag.getString("Id").isBlank() || tag.getString("Dimension").isBlank()) {
            return Optional.empty();
        }
        UUID active = tag.hasUUID("ActiveEncounter") ? tag.getUUID("ActiveEncounter") : null;
        BlockPos center=new BlockPos(tag.getInt("MinX")+(tag.getInt("MaxX")-tag.getInt("MinX"))/2,
                tag.getInt("MinY")+(tag.getInt("MaxY")-tag.getInt("MinY"))/2,
                tag.getInt("MinZ")+(tag.getInt("MaxZ")-tag.getInt("MinZ"))/2);
        BlockPos respawn=tag.contains("RespawnX")&&tag.contains("RespawnY")&&tag.contains("RespawnZ")
                ? new BlockPos(tag.getInt("RespawnX"),tag.getInt("RespawnY"),tag.getInt("RespawnZ")) : center;
        return Optional.of(new ArenaRecord(tag.getString("Id"), profile, tag.getString("Dimension"), tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"), respawn, tag.getBoolean("StructureArena"), tag.getBoolean("Unlocked"), active, tag.getLong("ActiveSince")));
    }
}
