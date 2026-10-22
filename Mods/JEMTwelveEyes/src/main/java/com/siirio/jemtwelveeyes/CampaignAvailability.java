package com.siirio.jemtwelveeyes;

import com.siirio.jemcompat.gate.CampaignBoss;
import com.siirio.jemcompat.gate.CampaignPrerequisite;
import com.siirio.jemcompat.gate.CampaignSavedData;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Availability never edits the strict roster or converts an active world to vanilla. */
public final class CampaignAvailability {
    private CampaignAvailability() { }

    public static List<String> missingRegistrations() {
        var missing = new LinkedHashSet<String>();
        for (var boss : CampaignBoss.values()) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(boss.entity())) missing.add(boss.entity().toString());
            if (!ForgeRegistries.ITEMS.containsKey(boss.eye())) missing.add(boss.eye().toString());
        }
        for (var prerequisite : CampaignPrerequisite.values()) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(prerequisite.entity())) missing.add(prerequisite.entity().toString());
        }
        return List.copyOf(missing);
    }

    public static List<String> missing(MinecraftServer server) {
        var missing = new LinkedHashSet<String>(missingRegistrations());
        for (var target : com.siirio.jemcompat.discovery.CampaignTarget.values()) {
            if (target.generatedSite()) continue;
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, target.dimension()));
            if (level == null) {
                missing.add(target.dimension().toString());
                continue;
            }
            var tag = net.minecraft.tags.TagKey.create(Registries.STRUCTURE, target.structureTag());
            if (level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(tag).filter(values -> values.size() > 0).isEmpty()) {
                missing.add("#" + target.structureTag());
            }
        }
        return List.copyOf(missing);
    }

    public static boolean playable(MinecraftServer server) {
        return CampaignSavedData.get(server).active() && missing(server).isEmpty();
    }

    public static boolean gatesPortal(MinecraftServer server) {
        return CampaignSavedData.get(server).active();
    }

    public static void explain(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.jemcompat.campaign.unavailable", String.join(", ", missing(player.server))));
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        var server = event.getServer();
        var missing = missing(server);
        if (!missing.isEmpty()) com.mojang.logging.LogUtils.getLogger().warn(
                "Twelve Eyes {}: missing {}", gatesPortal(server) ? "active world is locked until content returns" : "inactive in this new world; vanilla End remains available", missing);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !playable(player.server)) explain(player);
    }
}
