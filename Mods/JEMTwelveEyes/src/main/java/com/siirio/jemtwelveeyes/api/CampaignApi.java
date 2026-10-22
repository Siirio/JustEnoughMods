package com.siirio.jemtwelveeyes.api;

import com.siirio.jemcompat.gate.CampaignGateEvents;
import com.siirio.jemcompat.gate.CampaignSavedData;
import com.siirio.jemcompat.discovery.CampaignTarget;
import com.siirio.jemcompat.site.CampaignSiteService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CampaignApi {
    private CampaignApi() {
    }

    public static boolean isActive(MinecraftServer server) { return CampaignSavedData.get(server).active(); }
    public static boolean hasRequiredContent() { return com.siirio.jemtwelveeyes.CampaignAvailability.missingRegistrations().isEmpty(); }
    public static boolean isAvailable(MinecraftServer server) { return com.siirio.jemtwelveeyes.CampaignAvailability.playable(server); }
    public static boolean mainDefeated(MinecraftServer server, ResourceLocation entity) {
        return com.siirio.jemcompat.gate.CampaignBoss.byEntity(entity).isPresent() && CampaignSavedData.get(server).defeated(entity);
    }
    public static boolean complete(MinecraftServer server) {
        return isAvailable(server) && java.util.Arrays.stream(com.siirio.jemcompat.gate.CampaignBoss.values())
                .allMatch(boss -> CampaignSavedData.get(server).defeated(boss.entity()));
    }

    public static net.minecraft.network.chat.Component startBlockReason(MinecraftServer server, ResourceLocation entity) {
        if (com.siirio.jemtwelveeyes.CampaignAvailability.gatesPortal(server)
                && entity.equals(com.siirio.jemtwelveeyes.postend.PostEndTarget.OBLITERATOR.entity())
                && !com.siirio.jemtwelveeyes.postend.PostEndEvents.ready(server)) {
            return net.minecraft.network.chat.Component.translatable("message.jemcompat.post_end.required_bosses");
        }
        if (!locked(server, entity)) return null;
        if (!com.siirio.jemtwelveeyes.CampaignAvailability.playable(server)) {
            return net.minecraft.network.chat.Component.translatable("message.jemcompat.campaign.unavailable",
                    String.join(", ", com.siirio.jemtwelveeyes.CampaignAvailability.missing(server)));
        }
        var data = CampaignSavedData.get(server);
        var boss = com.siirio.jemcompat.gate.CampaignBoss.byEntity(entity).orElseThrow();
        var names = net.minecraft.network.chat.Component.empty();
        boss.prerequisites().stream().filter(value -> !data.prerequisiteDefeated(value)).forEach(value -> {
            if (!names.getString().isEmpty()) names.append(", ");
            names.append(net.minecraft.network.chat.Component.translatable(
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(value.entity()).getDescriptionId()));
        });
        return net.minecraft.network.chat.Component.translatable("message.jemcompat.campaign.required", names);
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

    public static List<BossLocation> locatedBosses(MinecraftServer server) {
        CampaignSavedData data = CampaignSavedData.get(server);
        List<BossLocation> locations = new ArrayList<>();
        for (CampaignTarget target : CampaignTarget.values()) {
            BlockPos position = target.generatedSite() ? data.campaignSite(target.prerequisite()) : data.locatedStructure(target.selectionKey());
            if (position != null) locations.add(new BossLocation(target.selectionKey(), target.entity(), target.dimension(), position));
        }
        return List.copyOf(locations);
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
