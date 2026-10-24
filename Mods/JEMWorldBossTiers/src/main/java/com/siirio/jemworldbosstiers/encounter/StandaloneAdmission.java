package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.RaidArenaApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
import com.siirio.jemworldbosstiers.network.TierNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** The domain remains playable when the optional hosted-server menus are absent. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class StandaloneAdmission {
    private record Pending(UUID token, UUID boss, long expires) { }
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private StandaloneAdmission() { }

    private static boolean ownsAdmission() { return !ModList.get().isLoaded("jem_server"); }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void attack(LivingAttackEvent event) {
        if (!ownsAdmission() || !HostedEncounterApi.enabled(event.getEntity().getServer()) || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !HostedEncounterApi.canHost(event.getEntity()) || HostedEncounters.isHosted(event.getEntity())) return;
        event.setCanceled(true);
        LivingEntity boss = event.getEntity();
        com.siirio.jemworldbosstiers.api.StructureEncounterApi.bind(boss);
        if (player.isSpectator() || !player.isAlive() || player.level() != boss.level() || player.distanceToSqr(boss) > 256) return;
        HostedEncounterApi.hold(boss);
        Pending previous = PENDING.get(player.getUUID());
        long now = player.server.overworld().getGameTime();
        if (previous != null && previous.boss().equals(boss.getUUID()) && previous.expires() > now) return;
        Pending pending = new Pending(UUID.randomUUID(), boss.getUUID(), now + 1200);
        PENDING.put(player.getUUID(), pending);
        Component reason = CampaignAdmission.blocked(player, boss);
        TierNetwork.openAdmission(player, pending.token(), boss.getDisplayName(), reason,
                false);
    }

    private static boolean raidAllowed(ServerPlayer player, LivingEntity boss) {
        var arena = RaidArenaApi.arena(boss).orElse(null);
        if (arena == null) return false;
        var box = player.getBoundingBox();
        var bounds = arena.bounds();
        boolean inside = box.minX >= bounds.minX() && box.maxX <= bounds.maxX() + 1
                && box.minY >= bounds.minY() && box.maxY <= bounds.maxY() + 1
                && box.minZ >= bounds.minZ() && box.maxZ <= bounds.maxZ() + 1;
        return inside && !RaidArenaApi.participated(player.server, arena.id(), player.getUUID())
                && HostedEncounterApi.canPrepareRaid(boss);
    }

    public static void choose(ServerPlayer player, UUID token, int choice) {
        if (!ownsAdmission() || !HostedEncounterApi.enabled(player.server)) return;
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.token().equals(token)) return;
        PENDING.remove(player.getUUID());
        if (choice == 0 || pending.expires() < player.server.overworld().getGameTime()
                || !player.isAlive() || player.isSpectator() || HostedEncounterApi.participating(player)) return;
        if (!(player.serverLevel().getEntity(pending.boss()) instanceof LivingEntity boss)
                || !boss.isAlive() || !HostedEncounterApi.canHost(boss) || HostedEncounters.isHosted(boss)
                || player.distanceToSqr(boss) > 256) return;
        Component blocked = CampaignAdmission.blocked(player, boss);
        if (blocked != null) { player.sendSystemMessage(blocked); return; }
        if (choice != 1) return;
        boss.getPersistentData().putBoolean("jem_solo", true);
        if (!HostedEncounterApi.start(boss, List.of(player.getUUID()), false)) {
            HostedEncounterApi.hold(boss);
            return;
        }
        player.sendSystemMessage(Component.translatable("message.jem_world_boss_tiers.encounter.started"));
    }

    @SubscribeEvent
    public static void closed(HostedEncounterClosedEvent event) {
        if (!ownsAdmission() || !event.raid()) return;
        var nativeData = event.frozenState().getCompound("NativeEncounter");
        var type = net.minecraft.resources.ResourceLocation.tryParse(event.frozenState().getString("EntityType"));
        if (type != null && nativeData.contains("ArenaId") && !RaidArenaApi.queueRaidCompletion(
                event.server(), nativeData.getString("ArenaId"), event.bossId(), type))
            throw new IllegalStateException("Raid completion was not persisted");
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PENDING.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppingEvent event) { PENDING.clear(); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!ownsAdmission() || event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity boss)
                || !HostedEncounterApi.isRaid(boss) || HostedEncounters.isHosted(boss)) return;
        if (HostedEncounterApi.preservesRaidBoss(boss) && HostedEncounterApi.wasNativeBeforeRaid(boss)) {
            HostedEncounterApi.restorePreparedRaid(boss);
            HostedEncounterApi.hold(boss);
        } else {
            WorldTierApi.encounter(boss).filter(value -> value.arenaId() != null).ifPresent(value ->
                    RaidArenaApi.queueRaidCompletion(boss.getServer(), value.arenaId(), boss.getUUID(),
                            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())));
        }
    }
}
