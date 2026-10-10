package com.siirio.jemtwelveeyes.api;

import com.siirio.jemcompat.gate.CampaignGateEvents;
import com.siirio.jemcompat.gate.CampaignSavedData;
import com.siirio.jemcompat.site.CampaignSiteService;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CampaignApi {
    private CampaignApi() {
    }

    public static boolean unlocked(ServerPlayer player, ResourceLocation entity) {
        return CampaignGateEvents.unlocked(player, entity);
    }

    public static boolean locked(MinecraftServer server, ResourceLocation entity) {
        return CampaignGateEvents.locked(server, entity);
    }

    public static void explainLocked(ServerPlayer player, ResourceLocation entity) {
        CampaignGateEvents.explainLocked(player, entity);
    }

    public static List<CampaignTerritory> generatedSites(MinecraftServer server) {
        return CampaignSiteService.territories(server);
    }

    public static boolean startSecretEnding(MinecraftServer server, UUID player) {
        return CampaignSavedData.get(server).startSecretEnding(player);
    }

    public static boolean revealPostEnd(MinecraftServer server) {
        return CampaignSavedData.get(server).revealPostEnd();
    }

    public static boolean postEndRevealed(MinecraftServer server) {
        return CampaignSavedData.get(server).postEndRevealed();
    }

    public static boolean recordPostEndDefeat(MinecraftServer server, ResourceLocation entity) {
        return CampaignSavedData.get(server).recordPostEndDefeat(entity);
    }

    public static boolean postEndDefeated(MinecraftServer server, ResourceLocation entity) {
        return CampaignSavedData.get(server).postEndDefeated(entity);
    }

    public static BlockPos locatedStructure(MinecraftServer server, String target) {
        return CampaignSavedData.get(server).locatedStructure(target);
    }

    public static void locatedStructure(MinecraftServer server, String target, BlockPos position) {
        CampaignSavedData.get(server).locatedStructure(target, position);
    }
}
