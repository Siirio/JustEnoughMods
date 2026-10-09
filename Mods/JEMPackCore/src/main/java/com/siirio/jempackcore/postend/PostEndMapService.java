package com.siirio.jempackcore.postend;

import com.siirio.jemcompat.gate.CampaignSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class PostEndMapService {
    private static final int SEARCH_RADIUS = 1024;
    private PostEndMapService() { }

    public static CompletableFuture<Optional<BlockPos>> locate(MinecraftServer server, PostEndTarget target) {
        var level = server.getLevel(Level.END);
        if (level == null) return CompletableFuture.completedFuture(Optional.empty());
        String key = "post_end/" + target.key();
        BlockPos cached = CampaignSavedData.get(server).locatedStructure(key);
        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, new ResourceLocation("jem_twelve_eyes", "post_end/" + target.key()));
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(level.findNearestMapStructure(tag, level.getSharedSpawnPos(), SEARCH_RADIUS, false)), Util.backgroundExecutor())
                .thenApplyAsync(result -> { result.ifPresent(pos -> CampaignSavedData.get(server).locatedStructure(key, pos)); return result; }, server);
    }
}
