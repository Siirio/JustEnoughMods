package com.siirio.jemcompat.site;

import com.siirio.jemcompat.gate.CampaignPrerequisite;
import com.siirio.jemtwelveeyes.api.CampaignTerritory;
import com.siirio.jemcompat.gate.CampaignSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CampaignSiteService {
    private static final int SITE_RADIUS = 4;
    private static final int STRUCTURE_MARGIN = 32;
    private static final int SITE_SPACING = 256;
    private static final int PLAYER_TRIGGER_DISTANCE = 32;
    private static final int PLAYER_CHECK_INTERVAL = 20;
    private static final int MAX_PLACEMENT_ATTEMPTS = 24;
    private static final Map<SearchKey, CompletableFuture<Optional<BlockPos>>> ACTIVE_SEARCHES = new ConcurrentHashMap<>();

    private CampaignSiteService() {
    }

    public static List<CampaignTerritory> territories(MinecraftServer server) {
        CampaignSavedData data = CampaignSavedData.get(server);
        List<CampaignTerritory> territories = new ArrayList<>();
        for (CampaignPrerequisite prerequisite : CampaignPrerequisite.values()) {
            if (!prerequisite.generatedSite()) continue;
            BlockPos site = data.campaignSite(prerequisite);
            if (site != null) territories.add(new CampaignTerritory(prerequisite.dimension(), prerequisite.entity(),
                    site.getX() - SITE_RADIUS, site.getZ() - SITE_RADIUS, site.getX() + SITE_RADIUS, site.getZ() + SITE_RADIUS));
        }
        return List.copyOf(territories);
    }

    @SubscribeEvent
    public static void spawnSiteBoss(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide()
                || event.player.tickCount % PLAYER_CHECK_INTERVAL != 0
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        CampaignSavedData data = CampaignSavedData.get(player.server);
        for (CampaignPrerequisite prerequisite : CampaignPrerequisite.values()) {
            if (!prerequisite.generatedSite()
                    || data.prerequisiteDefeated(prerequisite)
                    || !prerequisite.dimension().equals(player.level().dimension().location())) {
                continue;
            }
            BlockPos site = data.campaignSite(prerequisite);
            if (site == null || !site.closerToCenterThan(player.position(), PLAYER_TRIGGER_DISTANCE)) {
                continue;
            }
            UUIDState state = entityState(player.serverLevel(), data, prerequisite);
            if (state == UUIDState.LOADED || state == UUIDState.UNLOADED) {
                continue;
            }
            spawn(player.serverLevel(), data, prerequisite, site);
        }
    }

    public static CompletableFuture<Optional<BlockPos>> locateAsync(MinecraftServer server, CampaignPrerequisite prerequisite) {
        BlockPos existing = CampaignSavedData.get(server).campaignSite(prerequisite);
        if (existing != null) {
            return CompletableFuture.completedFuture(Optional.of(existing));
        }
        SearchKey key = new SearchKey(server, prerequisite);
        return ACTIVE_SEARCHES.computeIfAbsent(key, ignored -> placeAsync(server, prerequisite, 0)
                .whenComplete((result, error) -> ACTIVE_SEARCHES.remove(key)));
    }

    private static CompletableFuture<Optional<BlockPos>> placeAsync(MinecraftServer server,
                                                                     CampaignPrerequisite prerequisite,
                                                                     int attempt) {
        if (attempt >= MAX_PLACEMENT_ATTEMPTS) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, prerequisite.dimension());
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        BlockPos candidate = candidate(level, prerequisite, attempt);
        return loadCandidateChunks(level, candidate).thenComposeAsync(ignored -> {
            if (level.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4) == null) {
                return placeAsync(server, prerequisite, attempt + 1);
            }
            int y = siteY(level, prerequisite, candidate);
            BlockPos site = new BlockPos(candidate.getX(), y, candidate.getZ());
            BoundingBox area = siteArea(site);
            if (!biomeMatches(level, prerequisite, site)
                    || overlapsCampaignSite(server, prerequisite, site)
                    || overlapsLoadedStructure(level, area.inflatedBy(STRUCTURE_MARGIN))) {
                return placeAsync(server, prerequisite, attempt + 1);
            }
            buildSite(level, prerequisite, site);
            CampaignSavedData.get(server).campaignSite(prerequisite, site);
            return CompletableFuture.completedFuture(Optional.of(site));
        }, server);
    }

    private static BlockPos candidate(ServerLevel level, CampaignPrerequisite prerequisite, int attempt) {
        int ordinal = generatedOrdinal(prerequisite);
        long mixed = level.getSeed() ^ ((long) prerequisite.key().hashCode() << 32) ^ attempt * 0x9E3779B97F4A7C15L;
        double angle = Math.floorMod(mixed, 6_283L) / 1_000.0D + ordinal * 0.73D;
        int radius = 2_500 + ordinal * 300 + attempt * 50;
        BlockPos spawn = level.getSharedSpawnPos();
        return new BlockPos(
                spawn.getX() + (int) Math.round(Math.cos(angle) * radius),
                prerequisite.dimension().equals(Level.NETHER.location()) ? 64 : spawn.getY(),
                spawn.getZ() + (int) Math.round(Math.sin(angle) * radius)
        );
    }

    private static CompletableFuture<Void> loadCandidateChunks(ServerLevel level, BlockPos candidate) {
        int radius = SITE_RADIUS + STRUCTURE_MARGIN;
        ChunkPos minimum = new ChunkPos((candidate.getX() - radius) >> 4, (candidate.getZ() - radius) >> 4);
        ChunkPos maximum = new ChunkPos((candidate.getX() + radius) >> 4, (candidate.getZ() + radius) >> 4);
        ChunkPos center = new ChunkPos(candidate);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int x = minimum.x; x <= maximum.x; x++) {
            for (int z = minimum.z; z <= maximum.z; z++) {
                ChunkStatus status = x == center.x && z == center.z ? ChunkStatus.FULL : ChunkStatus.STRUCTURE_REFERENCES;
                futures.add(level.getChunkSource().getChunkFuture(x, z, status, true));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static BoundingBox siteArea(BlockPos site) {
        return new BoundingBox(
                site.getX() - SITE_RADIUS,
                site.getY() - 2,
                site.getZ() - SITE_RADIUS,
                site.getX() + SITE_RADIUS,
                site.getY() + 6,
                site.getZ() + SITE_RADIUS
        );
    }

    private static boolean biomeMatches(ServerLevel level, CampaignPrerequisite prerequisite, BlockPos site) {
        if (!aquatic(prerequisite)) {
            return true;
        }
        Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(site);
        return biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
    }

    private static int siteY(ServerLevel level, CampaignPrerequisite prerequisite, BlockPos candidate) {
        if (prerequisite.dimension().equals(Level.NETHER.location())) {
            for (int y = 96; y >= 32; y--) {
                BlockPos floor = new BlockPos(candidate.getX(), y, candidate.getZ());
                if (level.getBlockState(floor).isSolid()
                        && level.getBlockState(floor.above()).isAir()
                        && level.getBlockState(floor.above(2)).isAir()) {
                    return y + 1;
                }
            }
            return 64;
        }
        Heightmap.Types heightmap = aquatic(prerequisite) ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        return level.getHeight(heightmap, candidate.getX(), candidate.getZ());
    }

    private static boolean overlapsCampaignSite(MinecraftServer server, CampaignPrerequisite current, BlockPos candidate) {
        CampaignSavedData data = CampaignSavedData.get(server);
        long minimum = (long) SITE_SPACING * SITE_SPACING;
        return Arrays.stream(CampaignPrerequisite.values())
                .filter(CampaignPrerequisite::generatedSite)
                .filter(prerequisite -> prerequisite != current)
                .filter(prerequisite -> prerequisite.dimension().equals(current.dimension()))
                .map(data::campaignSite)
                .filter(position -> position != null)
                .anyMatch(position -> position.distSqr(candidate) < minimum);
    }

    private static boolean overlapsLoadedStructure(ServerLevel level, BoundingBox area) {
        ChunkPos minimum = new ChunkPos(area.minX() >> 4, area.minZ() >> 4);
        ChunkPos maximum = new ChunkPos(area.maxX() >> 4, area.maxZ() >> 4);
        for (int x = minimum.x; x <= maximum.x; x++) {
            for (int z = minimum.z; z <= maximum.z; z++) {
                for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(x, z), structure -> true)) {
                    if (start.isValid() && start.getBoundingBox().intersects(area)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void buildSite(ServerLevel level, CampaignPrerequisite prerequisite, BlockPos center) {
        Block floor = floorBlock(prerequisite);
        boolean aquatic = aquatic(prerequisite);
        for (int x = -SITE_RADIUS; x <= SITE_RADIUS; x++) {
            for (int z = -SITE_RADIUS; z <= SITE_RADIUS; z++) {
                level.setBlock(center.offset(x, -1, z), floor.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (!aquatic && Math.abs(x) <= 2 && Math.abs(z) <= 2) {
                    for (int y = 0; y <= 4; y++) {
                        level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        level.setBlock(center.offset(0, -1, 0), markerBlock(prerequisite).defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static Block floorBlock(CampaignPrerequisite prerequisite) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(prerequisite.floor());
    }

    private static Block markerBlock(CampaignPrerequisite prerequisite) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(prerequisite.marker());
    }

    private static boolean aquatic(CampaignPrerequisite prerequisite) {
        return prerequisite.aquatic();
    }

    private static int generatedOrdinal(CampaignPrerequisite prerequisite) {
        int ordinal = 0;
        for (CampaignPrerequisite value : CampaignPrerequisite.values()) {
            if (!value.generatedSite() || !value.dimension().equals(prerequisite.dimension())) {
                continue;
            }
            if (value == prerequisite) {
                return ordinal;
            }
            ordinal++;
        }
        return ordinal;
    }

    private static UUIDState entityState(ServerLevel level, CampaignSavedData data, CampaignPrerequisite prerequisite) {
        java.util.UUID entityId = data.campaignSiteEntity(prerequisite);
        if (entityId == null) {
            return UUIDState.MISSING;
        }
        Entity entity = level.getEntity(entityId);
        if (entity != null && entity.isAlive()) {
            return UUIDState.LOADED;
        }
        return level.getChunkSource().getChunkNow(data.campaignSite(prerequisite).getX() >> 4, data.campaignSite(prerequisite).getZ() >> 4) == null
                ? UUIDState.UNLOADED
                : UUIDState.MISSING;
    }

    private static void spawn(ServerLevel level, CampaignSavedData data, CampaignPrerequisite prerequisite, BlockPos site) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(prerequisite.entity());
        Entity entity = type.create(level);
        if (entity == null) {
            return;
        }
        entity.moveTo(site.getX() + 0.5D, site.getY() + 1.0D, site.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(site), MobSpawnType.STRUCTURE, null, null);
            mob.setPersistenceRequired();
        }
        if (level.addFreshEntity(entity)) {
            data.campaignSiteEntity(prerequisite, entity.getUUID());
        }
    }

    private enum UUIDState {
        MISSING,
        LOADED,
        UNLOADED
    }

    private record SearchKey(MinecraftServer server, CampaignPrerequisite prerequisite) {
    }
}
