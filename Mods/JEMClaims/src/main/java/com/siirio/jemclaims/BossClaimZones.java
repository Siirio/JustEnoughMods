package com.siirio.jemclaims;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.ModList;

public final class BossClaimZones extends SavedData {
    private static final int MARGIN = 10;
    private static final TagKey<Structure> STRUCTURES = TagKey.create(Registries.STRUCTURE, new ResourceLocation("jem_claims", "boss_territories"));
    private final Map<String, Zone> zones = new HashMap<>();
    private record Zone(String dimension, int minX, int minZ, int maxX, int maxZ) {
        boolean overlaps(String dimension, int minX, int minZ, int maxX, int maxZ) {
            return this.dimension.equals(dimension) && (long) this.minX - MARGIN <= maxX && (long) this.maxX + MARGIN >= minX
                    && (long) this.minZ - MARGIN <= maxZ && (long) this.maxZ + MARGIN >= minZ;
        }
    }

    public static BossClaimZones get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BossClaimZones::load, BossClaimZones::new, "jem_claim_zones");
    }

    public void observe(ServerLevel level, LevelChunk chunk) {
        chunk.getAllStarts().forEach((structure, start) -> {
            if (protectedStructure(level, structure) && start.isValid()) remember(level, structure, start);
        });
    }

    public boolean validate(ServerPlayer player, BlockPos first, BlockPos second) { return validate(player, first, second, true); }

    public boolean validate(ServerPlayer player, BlockPos first, BlockPos second, boolean notify) {
        ServerLevel level = player.serverLevel();
        if (ModList.get().isLoaded("jem_twelve_eyes")) CampaignZones.observe(this, player.server);
        int minX = Math.min(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxZ = Math.max(first.getZ(), second.getZ());
        String dimension = level.dimension().location().toString();
        if (overlaps(dimension, minX, minZ, maxX, maxZ)) return notify && protectedMessage(player);
        for (int x = (minX - MARGIN) >> 4; x <= (maxX + MARGIN) >> 4; x++) {
            for (int z = (minZ - MARGIN) >> 4; z <= (maxZ + MARGIN) >> 4; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) return notify && incompleteMessage(player);
                observe(level, chunk);
                for (var reference : chunk.getAllReferences().entrySet()) {
                    Structure structure = reference.getKey();
                    if (!protectedStructure(level, structure)) continue;
                    for (long packed : reference.getValue()) {
                        ChunkPos position = new ChunkPos(packed);
                        String key = key(level, structure, position);
                        if (zones.containsKey(key)) continue;
                        LevelChunk origin = level.getChunkSource().getChunkNow(position.x, position.z);
                        if (origin == null) return notify && incompleteMessage(player);
                        StructureStart start = origin.getStartForStructure(structure);
                        if (start == null || !start.isValid()) return notify && incompleteMessage(player);
                        remember(level, structure, start);
                    }
                }
            }
        }
        return !overlaps(dimension, minX, minZ, maxX, maxZ) || notify && protectedMessage(player);
    }

    private boolean overlaps(String dimension, int minX, int minZ, int maxX, int maxZ) {
        return zones.values().stream().anyMatch(zone -> zone.overlaps(dimension, minX, minZ, maxX, maxZ));
    }

    private static boolean protectedStructure(ServerLevel level, Structure structure) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return registry.getResourceKey(structure).flatMap(registry::getHolder).map(holder -> holder.is(STRUCTURES)).orElse(false);
    }

    private static String key(ServerLevel level, Structure structure, ChunkPos position) {
        return level.dimension().location() + "|" + level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure) + "|" + position.toLong();
    }

    private void remember(ServerLevel level, Structure structure, StructureStart start) {
        var box = start.getBoundingBox();
        put(key(level, structure, start.getChunkPos()), new Zone(level.dimension().location().toString(), box.minX(), box.minZ(), box.maxX(), box.maxZ()));
    }

    private void put(String key, Zone zone) {
        Zone existing = zones.get(key);
        Zone union = existing == null ? zone : new Zone(zone.dimension(), Math.min(existing.minX(), zone.minX()), Math.min(existing.minZ(), zone.minZ()), Math.max(existing.maxX(), zone.maxX()), Math.max(existing.maxZ(), zone.maxZ()));
        if (!union.equals(existing)) {
            zones.put(key, union);
            setDirty();
        }
    }

    private static boolean protectedMessage(ServerPlayer player) {
        return ClaimSelectionGuard.deny(player, "Нельзя приватить территорию босса и полосу в 10 блоков вокруг неё, даже после победы.");
    }

    private static boolean incompleteMessage(ServerPlayer player) {
        return ClaimSelectionGuard.deny(player, "Данные соседних структур ещё не загружены. Подойдите ближе к выделенной области и повторите попытку.");
    }

    private static BossClaimZones load(CompoundTag root) {
        BossClaimZones data = new BossClaimZones();
        for (Tag raw : root.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            data.zones.put(tag.getString("Key"), new Zone(tag.getString("Dimension"), tag.getInt("MinX"), tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxZ")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        zones.forEach((key, zone) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Key", key);
            tag.putString("Dimension", zone.dimension());
            tag.putInt("MinX", zone.minX());
            tag.putInt("MinZ", zone.minZ());
            tag.putInt("MaxX", zone.maxX());
            tag.putInt("MaxZ", zone.maxZ());
            entries.add(tag);
        });
        root.put("Zones", entries);
        return root;
    }

    private static final class CampaignZones {
        private static final int SITE_RADIUS = 4;

        private static void observe(BossClaimZones zones, MinecraftServer server) {
            var data = com.siirio.jemcompat.gate.CampaignSavedData.get(server);
            for (var prerequisite : com.siirio.jemcompat.gate.CampaignPrerequisite.values()) {
                if (!prerequisite.generatedSite()) continue;
                BlockPos site = data.campaignSite(prerequisite);
                if (site == null) continue;
                String dimension = prerequisite.dimension().toString();
                zones.put("site|" + dimension + "|" + prerequisite.entity(), new Zone(dimension, site.getX() - SITE_RADIUS, site.getZ() - SITE_RADIUS, site.getX() + SITE_RADIUS, site.getZ() + SITE_RADIUS));
            }
        }
    }
}
