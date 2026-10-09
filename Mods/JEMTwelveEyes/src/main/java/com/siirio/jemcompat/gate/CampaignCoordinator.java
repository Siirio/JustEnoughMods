package com.siirio.jemcompat.gate;

import com.siirio.jemtwelveeyes.integration.WorldTierParticipants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class CampaignCoordinator {
    public CampaignCoordinator() {
        MinecraftForge.EVENT_BUS.register(new CampaignEyeProtection());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void defeated(LivingDeathEvent event) {
        if (raid(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        var entityId = RegistryIds.entity(event.getEntity());
        recordPrerequisite(level, entityId);
        CampaignBoss boss = CampaignBoss.byEntity(entityId).orElse(null);
        if (boss == null) {
            return;
        }
        CampaignSavedData global = CampaignSavedData.get(level.getServer());
        Set<UUID> participants = ModList.get().isLoaded("jem_world_boss_tiers")
                ? WorldTierParticipants.forEntity(event.getEntity()) : new LinkedHashSet<>();
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            participants.add(killer.getUUID());
        }
        boolean firstDefeat = global.recordDefeat(boss.entity());
        CampaignEyeService.deliver(level, event.getEntity().position(), boss, participants);
        if (!firstDefeat) {
            return;
        }
        GateAdvancements.awardDefeat(level, event.getEntity().position(), boss, participants);
        boolean complete = java.util.Arrays.stream(CampaignBoss.values()).allMatch(value -> global.defeated(value.entity()));
        if (complete) {
            GateAdvancements.awardCampaign(level, participants);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void animatedPrerequisiteDefeated(LivingDeathEvent event) {
        if (raid(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        var entityId = RegistryIds.entity(event.getEntity());
        if (CampaignPrerequisite.byEntity(entityId).filter(CampaignPrerequisite::canceledDeathProof).isEmpty()) {
            return;
        }
        recordPrerequisite(level, entityId);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void commandKilledPrerequisite(LivingAttackEvent event) {
        if (raid(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)
                || !event.getSource().is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        recordPrerequisite(level, RegistryIds.entity(event.getEntity()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void lethallyDamagedPrerequisite(LivingDamageEvent event) {
        if (raid(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)
                || event.getAmount() < event.getEntity().getHealth()) {
            return;
        }
        recordPrerequisite(level, RegistryIds.entity(event.getEntity()));
    }

    @SubscribeEvent
    public void prerequisiteAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CampaignPrerequisite.byProofAdvancement(event.getAdvancement().getId())
                .or(() -> byCampaignAdvancement(event.getAdvancement().getId()))
                .ifPresent(prerequisite -> recordPrerequisite(player.serverLevel(), prerequisite.entity()));
    }

    @SubscribeEvent
    public void reconcilePrerequisiteAdvancements(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (CampaignPrerequisite prerequisite : CampaignPrerequisite.values()) {
            if (prerequisite.proofAdvancements().stream().anyMatch(id -> completed(player, id))
                    || completedCampaignAdvancement(player, prerequisite)) {
                recordPrerequisite(player.serverLevel(), prerequisite.entity());
            }
        }
    }

    private static boolean raid(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getBoolean("jem:event_spawned")
                || "RAID_EVENT".equals(entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance"));
    }

    private static java.util.Optional<CampaignPrerequisite> byCampaignAdvancement(net.minecraft.resources.ResourceLocation advancement) {
        for (CampaignBoss boss : CampaignBoss.values()) {
            for (CampaignPrerequisite prerequisite : boss.prerequisites()) {
                if (boss.prerequisiteAdvancement(prerequisite).equals(advancement)) {
                    return java.util.Optional.of(prerequisite);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean completedCampaignAdvancement(ServerPlayer player, CampaignPrerequisite prerequisite) {
        return java.util.Arrays.stream(CampaignBoss.values())
                .filter(boss -> boss.prerequisites().contains(prerequisite))
                .map(boss -> boss.prerequisiteAdvancement(prerequisite))
                .anyMatch(id -> completed(player, id));
    }

    private static boolean completed(ServerPlayer player, net.minecraft.resources.ResourceLocation advancementId) {
        net.minecraft.advancements.Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void recordPrerequisite(ServerLevel level, net.minecraft.resources.ResourceLocation entityId) {
        CampaignPrerequisite prerequisite = CampaignPrerequisite.byEntity(entityId).orElse(null);
        if (prerequisite == null) {
            return;
        }
        CampaignSavedData global = CampaignSavedData.get(level.getServer());
        if (!global.recordPrerequisite(prerequisite.entity())) {
            return;
        }
        for (CampaignBoss boss : CampaignBoss.values()) {
            if (!boss.prerequisites().contains(prerequisite)) {
                continue;
            }
            GateAdvancements.awardPrerequisite(level, boss, prerequisite);
            level.getServer().getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.jemcompat.campaign.prerequisite_recorded",
                            net.minecraft.network.chat.Component.translatable(
                                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(prerequisite.entity()).getDescriptionId()
                            ),
                            net.minecraft.network.chat.Component.translatable(
                                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(boss.entity()).getDescriptionId()
                            )
                    )
            ));
            if (!boss.prerequisites().stream().allMatch(global::prerequisiteDefeated) || !global.unlock(boss)) {
                continue;
            }
            level.getServer().getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.jemcompat.campaign.unlocked",
                            net.minecraft.network.chat.Component.translatable(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(boss.entity()).getDescriptionId())
                    )
            ));
        }
    }
}
