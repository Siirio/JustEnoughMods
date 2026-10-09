package com.siirio.jemcompat.discovery;

import com.siirio.jemcompat.site.CampaignSiteService;
import com.siirio.jemcompat.gate.CampaignSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CampaignMapService {
    private static final int SEARCH_RADIUS = 512;
    private static final int MAX_DISTANCE_BLOCKS = 8_192;
    private static final long MAX_DISTANCE_SQUARED = (long) MAX_DISTANCE_BLOCKS * MAX_DISTANCE_BLOCKS;
    private static final Map<SearchKey, CompletableFuture<Optional<Target>>> ACTIVE_SEARCHES = new ConcurrentHashMap<>();

    private CampaignMapService() {
    }

    public static CompletableFuture<Optional<Target>> locateAsync(MinecraftServer server, CampaignTarget target) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, target.dimension());
        ServerLevel targetLevel = server.getLevel(dimension);
        if (targetLevel == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (target.generatedSite()) {
            return CampaignSiteService.locateAsync(server, target.prerequisite())
                    .thenApply(position -> position.map(value -> new Target(value, dimension)));
        }
        BlockPos origin = targetLevel.getSharedSpawnPos();
        BlockPos cached = CampaignSavedData.get(server).locatedStructure(target.selectionKey());
        if (cached != null && horizontalDistanceSquared(origin, cached) <= MAX_DISTANCE_SQUARED) {
            return CompletableFuture.completedFuture(Optional.of(new Target(cached, dimension)));
        }
        TagKey<Structure> destination = TagKey.create(Registries.STRUCTURE, target.structureTag());
        SearchKey searchKey = new SearchKey(server, target.selectionKey());
        return ACTIVE_SEARCHES.computeIfAbsent(searchKey, ignored -> CompletableFuture.supplyAsync(() -> {
                    BlockPos targetPosition = targetLevel.findNearestMapStructure(destination, origin, SEARCH_RADIUS, false);
                    if (targetPosition == null || horizontalDistanceSquared(origin, targetPosition) > MAX_DISTANCE_SQUARED) {
                        return Optional.<Target>empty();
                    }
                    return Optional.of(new Target(targetPosition, dimension));
                }, Util.backgroundExecutor())
                .thenApplyAsync(result -> {
                    result.ifPresent(found -> CampaignSavedData.get(server)
                            .locatedStructure(target.selectionKey(), found.position()));
                    return result;
                }, server)
                .whenComplete((result, error) -> ACTIVE_SEARCHES.remove(searchKey)));
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long x = (long) first.getX() - second.getX();
        long z = (long) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    public record Target(BlockPos position, ResourceKey<Level> dimension) {
    }

    private record SearchKey(MinecraftServer server, String target) {
    }
}
