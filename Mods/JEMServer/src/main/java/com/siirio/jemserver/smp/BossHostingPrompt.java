package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Attack admission shares EventChoice/HostedParties; no proximity activation or arena immunity. */
@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BossHostingPrompt {
    private static final Map<UUID, Long> NEXT_PROMPT = new HashMap<>();

    public static boolean canOffer(ServerPlayer player, LivingEntity boss) {
        return SmpEnvironment.active(player) && player.isAlive() && !player.isSpectator()
                && player.level() == boss.level() && boss.isAlive() && !boss.isRemoved()
                && HostedEncounterApi.canHost(boss) && HostedEncounterApi.status(boss).isEmpty()
                && !StructureStaging.managed(boss) && BossStaging.contains(boss, player)
                && (!net.minecraftforge.fml.ModList.get().isLoaded("jem_twelve_eyes")
                || com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(player,
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())));
    }

    public static void prompt(ServerPlayer player, UUID id) {
        if (!(player.serverLevel().getEntity(id) instanceof LivingEntity boss)) throw new SmpActionFailure("unavailable");
        SmpRecords.require(HostedEncounterApi.canHost(boss), "unavailable");
        var raid = EventScheduler.active(player.server, "BOSS_RAID");
        if (raid != null && raid.hasUUID("targetBoss") && raid.getUUID("targetBoss").equals(id)) {
            EventAreaHooks.prompt(player, raid.getUUID("id"));
            return;
        }
        if (StructureStaging.prompt(player, boss)) return;
        SmpRecords.require(canOffer(player, boss), "unavailable");
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_twelve_eyes")
                && !com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(player, key)) {
            com.siirio.jemtwelveeyes.api.CampaignApi.explainLocked(player, key);
            return;
        }
        EventNetwork.prompt(player, id, EventEntryKind.BOSS_FIGHT, boss.getDisplayName().getString(), key.toString());
    }

    private static LivingEntity boss(Entity target) {
        while (target instanceof net.minecraftforge.entity.PartEntity<?> part) target = part.getParent();
        return target instanceof LivingEntity living && HostedEncounterApi.canHost(living) ? living : null;
    }

    private static void attempt(ServerPlayer player, LivingEntity boss) {
        if (!SmpEnvironment.active(player) || !player.isAlive() || player.isSpectator()
                || player.level() != boss.level() || !HostedEncounterApi.held(boss)
                || com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isEmpty()) return;
        long now = player.server.overworld().getGameTime();
        if (now < NEXT_PROMPT.getOrDefault(player.getUUID(), Long.MIN_VALUE)) return;
        NEXT_PROMPT.put(player.getUUID(), now + 40);
        try { prompt(player, boss.getUUID()); }
        catch (IllegalArgumentException failure) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error." + failure.getMessage()), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void melee(AttackEntityEvent event) {
        LivingEntity boss = boss(event.getTarget());
        if (event.getEntity() instanceof ServerPlayer player && boss != null && HostedEncounterApi.held(boss)) {
            event.setCanceled(true);
            attempt(player, boss);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void attacked(LivingAttackEvent event) {
        LivingEntity boss = boss(event.getEntity());
        if (boss == null || !HostedEncounterApi.held(boss)) return;
        event.setCanceled(true);
        Entity owner = HostedEncounterApi.damageOwner(event.getSource().getEntity());
        if (owner == null) owner = HostedEncounterApi.damageOwner(event.getSource().getDirectEntity());
        if (owner instanceof ServerPlayer player) attempt(player, boss);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof LivingEntity boss
                && SmpEnvironment.active(boss.getServer()) && HostedEncounterApi.canHost(boss)
                && !boss.getPersistentData().getBoolean(EventSession.SPAWNED)
                && HostedEncounterApi.status(boss).isEmpty()) HostedEncounterApi.hold(boss);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { NEXT_PROMPT.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { NEXT_PROMPT.clear(); BossStaging.clear(); }
    private BossHostingPrompt() {}
}
