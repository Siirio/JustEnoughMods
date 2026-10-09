package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.EventNetwork;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BossHostingPrompt {
    private static final int CANDIDATE_LIMIT = 4096;
    private static final int CHECKS_PER_PASS = 64;
    private static final int SEEN_PER_PLAYER = 64;
    private static final int TICKS_PER_SECOND = 20;
    private static final int PROMPT_COOLDOWN_TICKS = TICKS_PER_SECOND * 30;
    private static final String COMBAT_STARTED = "JEMHostingPromptCombatStarted";
    private static final LinkedHashMap<UUID, LivingEntity> CANDIDATES = new LinkedHashMap<>();
    private static final Map<UUID, LinkedHashSet<UUID>> SEEN = new HashMap<>();
    private static final Map<UUID, Long> LAST_PROMPT = new HashMap<>();

    public static boolean canOffer(ServerPlayer player, LivingEntity boss) {
        if (!SmpEnvironment.active(player)) return false;
        if (com.siirio.jemserver.smp.events.StructureStaging.managed(boss)) return false;
        if (!player.isAlive()
                || player.isSpectator()
                || player.level() != boss.level()
                || !boss.isAlive()
                || boss.isRemoved()
                || !HostedEncounterApi.status(boss).isEmpty()) return false;
        var profile = WorldTierApi.profile(boss).orElse(null);
        if (profile == null) return false;
        if (!com.siirio.jemserver.smp.events.BossStaging.contains(boss, player)) return false;
        return true;
    }

    public static void prompt(ServerPlayer player, UUID id) {
        if (!(player.serverLevel().getEntity(id) instanceof LivingEntity boss)) throw new IllegalArgumentException("unavailable");
        if(com.siirio.jemserver.smp.events.StructureStaging.prompt(player,boss)) {
            CANDIDATES.remove(id);
            com.siirio.jemserver.smp.events.BossStaging.remove(id);
            return;
        }
        if(!canOffer(player,boss)) throw new IllegalArgumentException("unavailable");
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        EventNetwork.prompt(player, id, "BOSS", boss.getDisplayName().getString(), key.toString());
    }

    private static void enroll(LivingEntity boss) {
        if (com.siirio.jemserver.smp.events.StructureStaging.managed(boss)) return;
        if (boss.level().isClientSide
                || !SmpEnvironment.active(boss.getServer())
                || boss.isRemoved()
                || !boss.isAlive()
                || WorldTierApi.profile(boss).isEmpty()
                || HostedEncounterApi.isRaid(boss)) return;
        if (CANDIDATES.containsKey(boss.getUUID())) return;
        if (CANDIDATES.size() >= CANDIDATE_LIMIT)
            CANDIDATES.remove(CANDIDATES.keySet().iterator().next());
        CANDIDATES.put(boss.getUUID(), boss);
        if (HostedEncounterApi.status(boss).isEmpty()) HostedEncounterApi.hold(boss);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity boss)
            enroll(boss);
    }

    @SubscribeEvent
    public static void watching(ChunkWatchEvent.Watch event) {
        ServerLevel level = event.getLevel();
        var position = event.getPos();
        if (level.getChunkSource().getChunkNow(position.x, position.z) == null) return;
        var bounds =
                new AABB(
                        position.getMinBlockX(),
                        level.getMinBuildHeight(),
                        position.getMinBlockZ(),
                        position.getMaxBlockX() + 1,
                        level.getMaxBuildHeight(),
                        position.getMaxBlockZ() + 1);
        level.getEntitiesOfClass(
                        LivingEntity.class,
                        bounds,
                        entity -> WorldTierApi.profile(entity).isPresent())
                .forEach(BossHostingPrompt::enroll);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        var boss = event.getEntity();
        if (!boss.level().isClientSide
                && event.getAmount() > 0
                && WorldTierApi.profile(boss).isPresent()) {
            boss.getPersistentData().putBoolean(COMBAT_STARTED, true);
            CANDIDATES.remove(boss.getUUID());
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) { CANDIDATES.remove(event.getEntity().getUUID()); com.siirio.jemserver.smp.events.BossStaging.remove(event.getEntity().getUUID()); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        var server = event.getServer();
        if (event.phase != TickEvent.Phase.END
                || !SmpEnvironment.active(server)
                || server.getTickCount() % TICKS_PER_SECOND != 0
                || CANDIDATES.isEmpty()) return;
        var linked = new HashSet<UUID>();
        for (var party : SmpData.get(server).all("parties")) {
            if (!SmpData.closed(party) && party.hasUUID("bossEntity"))
                linked.add(party.getUUID("bossEntity"));
        }
        int checks = Math.min(CHECKS_PER_PASS, CANDIDATES.size());
        for (int index = 0; index < checks; index++) {
            var entry = CANDIDATES.entrySet().iterator().next();
            UUID id = entry.getKey();
            LivingEntity boss = entry.getValue();
            CANDIDATES.remove(id);
            if (boss.isRemoved()
                    || !boss.isAlive()) continue;
            CANDIDATES.put(id, boss);
            if (linked.contains(id)) continue;
            if(HostedEncounterApi.status(boss).isEmpty()) HostedEncounterApi.hold(boss);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var seen = SEEN.get(player.getUUID());
                if (seen != null && seen.contains(id)) continue;
                long now = server.overworld().getGameTime();
                if (now - LAST_PROMPT.getOrDefault(player.getUUID(), now - PROMPT_COOLDOWN_TICKS)
                                < PROMPT_COOLDOWN_TICKS
                        || !canOffer(player, boss)) continue;
                player.sendSystemMessage(Component.literal("▣ ").withStyle(ChatFormatting.GOLD)
                        .append(boss.getDisplayName()).append(" ")
                        .append(Component.literal("[Открыть бой]").withStyle(style->style.withColor(ChatFormatting.GREEN).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp boss "+id)))));
                seen = SEEN.computeIfAbsent(player.getUUID(), key -> new LinkedHashSet<>());
                if (seen.size() >= SEEN_PER_PLAYER) seen.remove(seen.iterator().next());
                seen.add(id);
                LAST_PROMPT.put(player.getUUID(), now);
            }
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void stagingDamage(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && CANDIDATES.values().stream().anyMatch(boss->HostedEncounterApi.held(boss) && com.siirio.jemserver.smp.events.BossStaging.contains(boss,player))) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void stagingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && CANDIDATES.values().stream().anyMatch(boss->HostedEncounterApi.held(boss) && com.siirio.jemserver.smp.events.BossStaging.contains(boss,player))) { event.setCanceled(true);player.setHealth(Math.max(1,player.getHealth())); }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SEEN.remove(event.getEntity().getUUID());
        LAST_PROMPT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CANDIDATES.clear();
        com.siirio.jemserver.smp.events.BossStaging.clear();
        SEEN.clear();
        LAST_PROMPT.clear();
    }

    private BossHostingPrompt() {}
}
