package com.siirio.jemcompat.gate;

import com.siirio.jemtwelveeyes.AdvancementAwards;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class GateAdvancements {
    private static final double CREDIT_RADIUS = 64.0;

    private GateAdvancements() {
    }

    public static void awardDefeat(ServerLevel level, Vec3 position, CampaignBoss boss, Collection<UUID> participants) {
        award(level, position, boss.defeatAdvancement(), Set.copyOf(participants));
    }

    public static void awardCampaign(ServerLevel level, Collection<UUID> participants) {
        Set<UUID> preferred = Set.copyOf(participants);
        level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> preferred.isEmpty() || preferred.contains(player.getUUID()))
                .forEach(player -> AdvancementAwards.award(player, new ResourceLocation("jemcompat", "campaign/all_eyes")));
    }

    public static void awardPrerequisite(ServerLevel level, CampaignBoss boss, CampaignPrerequisite prerequisite) {
        level.getServer().getPlayerList().getPlayers()
                .forEach(player -> AdvancementAwards.award(player, boss.prerequisiteAdvancement(prerequisite)));
    }

    public static void awardCampaignEvent(ServerLevel level, Vec3 position, ResourceLocation advancement, ServerPlayer actor) {
        award(level, position, advancement, Set.of(actor.getUUID()));
    }

    private static void award(ServerLevel level, Vec3 position, ResourceLocation advancement, Set<UUID> preferred) {
        recipients(level, position, preferred).forEach(player -> AdvancementAwards.award(player, advancement));
    }

    static Set<ServerPlayer> recipients(ServerLevel level, Vec3 position, Collection<UUID> preferred) {
        Set<ServerPlayer> recipients = new LinkedHashSet<>();
        for (UUID id : preferred) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                recipients.add(player);
            }
        }
        AABB area = new AABB(position, position).inflate(CREDIT_RADIUS);
        recipients.addAll(level.getEntitiesOfClass(ServerPlayer.class, area));
        return recipients;
    }
}
