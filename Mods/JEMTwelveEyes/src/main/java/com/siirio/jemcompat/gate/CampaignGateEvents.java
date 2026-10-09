package com.siirio.jemcompat.gate;

import com.siirio.jemtwelveeyes.AdvancementAwards;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CampaignGateEvents {
    private static final long MESSAGE_COOLDOWN = 200L;
    private static final int TITLE_STAY_TICKS = 240;
    private static final int BARRIER_CHECK_INTERVAL = 1;
    private static final Map<UUID, Long> LAST_CHAT_MESSAGE = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_FULL_SCREEN_MESSAGE = new LinkedHashMap<>();

    private CampaignGateEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectLockedBoss(LivingAttackEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || event.getSource().is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        CampaignBoss boss = CampaignBoss.byEntity(RegistryIds.entity(event.getEntity())).orElse(null);
        if (boss == null || CampaignSavedData.get(level.getServer()).unlocked(boss)) {
            return;
        }
        event.setCanceled(true);
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            explain(player, boss);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectLockedInteraction(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        CampaignBoss boss = CampaignBoss.byEntity(RegistryIds.entity(event.getTarget())).orElse(null);
        if (boss == null || CampaignSavedData.get(level.getServer()).unlocked(boss)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        explain(player, boss);
    }

    @SubscribeEvent
    public static void restoreGlobalMilestones(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CampaignSavedData data = CampaignSavedData.get(player.server);
        LAST_CHAT_MESSAGE.remove(player.getUUID());
        LAST_FULL_SCREEN_MESSAGE.remove(player.getUUID());
        CampaignBarrierData.get(player.server).reconcile(player.server);
        for (CampaignBoss boss : CampaignBoss.values()) {
            boss.prerequisites().stream()
                    .filter(data::prerequisiteDefeated)
                    .forEach(prerequisite -> AdvancementAwards.award(player, boss.prerequisiteAdvancement(prerequisite)));
        }
    }

    @SubscribeEvent
    public static void clearPlayerState(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
        LAST_CHAT_MESSAGE.remove(player);
        LAST_FULL_SCREEN_MESSAGE.remove(player);
    }

    @SubscribeEvent
    public static void blockLockedStructureEntry(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)
                || player.level().isClientSide()
                || player.tickCount % BARRIER_CHECK_INTERVAL != 0) {
            return;
        }
        CampaignSavedData data = CampaignSavedData.get(player.server);
        CampaignBarrierData barriers=CampaignBarrierData.get(player.server);
        if(player.tickCount%20==0) barriers.reconcile(player.server);
        CampaignBarrierData.Barrier nearby=barriers.near(new CampaignBarrierData.ServerPlayerView(
                player.level().dimension().location(),player.getX(),player.getY(),player.getZ()));
        if(nearby!=null&&!data.unlocked(nearby.boss())) {
            explain(player,nearby.boss(),true);
            return;
        }
        for (CampaignBoss boss : CampaignBoss.values()) {
            if (boss.prerequisites().isEmpty()
                    || data.unlocked(boss)
                    || !boss.dimension().equals(player.level().dimension().location())) {
                continue;
            }
            TagKey<Structure> structures = TagKey.create(Registries.STRUCTURE, boss.structureTag());
            StructureStart start = player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), structures);
            if (!start.isValid()) {
                continue;
            }
            explain(player,boss,true);
            return;
        }
    }

    @SubscribeEvent
    public static void installLockedStructureBarrier(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunk=event.getChunk().getPos();
        CampaignSavedData progress=CampaignSavedData.get(level.getServer());
        CampaignBarrierData barriers=CampaignBarrierData.get(level.getServer());
        for(CampaignBoss boss:CampaignBoss.values()) {
            if(boss.prerequisites().isEmpty()||progress.unlocked(boss)||!boss.dimension().equals(level.dimension().location())) continue;
            TagKey<Structure> tag=TagKey.create(Registries.STRUCTURE,boss.structureTag());
            for(StructureStart start:level.structureManager().startsForStructure(chunk,structure->tagged(level,structure,tag))) {
                if(start.isValid()) barriers.ensure(level,boss,start.getBoundingBox());
            }
        }
    }

    private static void explain(ServerPlayer player, CampaignBoss boss) {
        explain(player, boss, false);
    }

    public static boolean unlocked(ServerPlayer player, net.minecraft.resources.ResourceLocation entity) {
        CampaignBoss boss = CampaignBoss.byEntity(entity).orElse(null);
        return boss == null || CampaignSavedData.get(player.server).unlocked(boss);
    }

    public static boolean locked(net.minecraft.server.MinecraftServer server,net.minecraft.resources.ResourceLocation entity) {
        CampaignBoss boss=CampaignBoss.byEntity(entity).orElse(null);
        return boss!=null&&!CampaignSavedData.get(server).unlocked(boss);
    }

    public static void explainLocked(ServerPlayer player, net.minecraft.resources.ResourceLocation entity) {
        CampaignBoss boss = CampaignBoss.byEntity(entity).orElse(null);
        if (boss != null && !CampaignSavedData.get(player.server).unlocked(boss)) explain(player, boss, true);
    }

    private static void explain(ServerPlayer player, CampaignBoss boss, boolean fullScreen) {
        long now = player.serverLevel().getGameTime();
        CampaignSavedData data = CampaignSavedData.get(player.server);
        List<CampaignPrerequisite> missing = boss.prerequisites().stream()
                .filter(requirement -> !data.prerequisiteDefeated(requirement))
                .toList();
        Component bossName = Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(boss.entity()).getDescriptionId());
        Component locked = Component.translatable("message.jemcompat.campaign.locked", bossName);
        Component required = Component.translatable("message.jemcompat.campaign.required", names(missing));
        if (ready(LAST_CHAT_MESSAGE, player.getUUID(), now)) {
            LAST_CHAT_MESSAGE.put(player.getUUID(), now);
            player.sendSystemMessage(locked);
            player.sendSystemMessage(required);
            missing.forEach(requirement -> player.sendSystemMessage(Component.translatable(requirement.hintKey())));
        }
        if (fullScreen && ready(LAST_FULL_SCREEN_MESSAGE, player.getUUID(), now)) {
            LAST_FULL_SCREEN_MESSAGE.put(player.getUUID(), now);
            CampaignNetwork.showGateWarning(player, locked, required, TITLE_STAY_TICKS);
        }
    }

    private static boolean ready(Map<UUID, Long> messages, UUID player, long now) {
        Long previous = messages.get(player);
        return previous == null || now - previous >= MESSAGE_COOLDOWN;
    }

    private static Component names(List<CampaignPrerequisite> requirements) {
        List<Component> names = new ArrayList<>();
        requirements.forEach(requirement -> names.add(Component.translatable(
                BuiltInRegistries.ENTITY_TYPE.get(requirement.entity()).getDescriptionId()
        )));
        MutableComponent result = Component.empty();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(names.get(index));
        }
        return result;
    }

    private static boolean tagged(ServerLevel level,Structure structure,TagKey<Structure> tag) {
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> key=registry.getResourceKey(structure).orElse(null);
        return key!=null&&registry.getHolder(key).map(holder->holder.is(tag)).orElse(false);
    }

}
