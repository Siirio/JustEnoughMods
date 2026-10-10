package com.siirio.jemserver;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(JemServer.MOD_ID)
public final class JemServer {
    public static final String MOD_ID = "jem_server";
    private static final String ATTACKER = "jem_server_attacker";
    private static final String DAMAGE_TIME = "jem_server_damage_time";
    private static final String COUNTED_ENTITY = "jem_server_counted_entity";
    private final SleepVote sleep;
    private final Map<UUID, PendingDeath> pendingDeaths;
    private boolean rosterChanged;
    private record PendingDeath(LivingEntity entity, UUID player, String playerName, ResourceLocation boss, ServerData.Death death) {}

    public JemServer() {
        boolean dedicated = net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER;
        sleep = dedicated ? new SleepVote() : null;
        pendingDeaths = dedicated ? new HashMap<>() : null;
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        EventDefaultPack.register();
        LandmarkNetwork.register();
        MapNetwork.register();
        ClaimBoundaryNetwork.register();
        com.siirio.jemserver.smp.SmpRuntime.register();
        if (dedicated && ModList.get().isLoaded("jem_world_boss_tiers")) MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerCommands(RegisterCommandsEvent event) {
        PackSyncCommands.register(event);
        var dispatcher = event.getDispatcher();
        for (String name : new String[]{"deaths", "death"}) {
            dispatcher.register(Commands.literal(name).executes(context -> {
                DeathHistory.open(context.getSource().getPlayerOrException());
                return 1;
            }));
        }
        dispatcher.register(Commands.literal("sleep")
                .then(Commands.literal("agree").executes(context -> sleep.vote(context.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("disagree").executes(context -> sleep.vote(context.getSource().getPlayerOrException(), false))));
        var landmarks = Commands.literal("landmarks").executes(context -> {
            Landmarks.open(context.getSource().getPlayerOrException(), 0, false);
            return 1;
        }).then(Commands.literal("create").executes(context -> {
            Landmarks.create(context.getSource().getPlayerOrException());
            return 1;
        })).then(Commands.literal("admin").requires(source -> source.hasPermission(2)).executes(context -> {
            Landmarks.open(context.getSource().getPlayerOrException(), 0, true);
            return 1;
        }));
        landmarks.then(Commands.literal("remove")
                .then(Commands.argument("id", UuidArgument.uuid()).executes(context -> Landmarks.remove(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "id")))));
        dispatcher.register(landmarks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void actualDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !ServerConfig.LEADERBOARD_ENABLED.get() || event.getAmount() <= 0
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || WorldTierApi.profile(event.getEntity()).isEmpty()) return;
        var tag = event.getEntity().getPersistentData();
        tag.putUUID(ATTACKER, player.getUUID());
        tag.putLong(DAMAGE_TIME, player.server.overworld().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void death(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.getPersistentData().hasUUID(COUNTED_ENTITY) && entity.getPersistentData().getUUID(COUNTED_ENTITY).equals(entity.getUUID())) return;
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            var record = new ServerData.Death(UUID.randomUUID(), player.level().dimension().location(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), System.currentTimeMillis(), event.getSource().getLocalizedDeathMessage(player).getString());
            pendingDeaths.putIfAbsent(entity.getUUID(), new PendingDeath(entity, player.getUUID(), player.getGameProfile().getName(), null, record));
            return;
        }
        if (!ServerConfig.LEADERBOARD_ENABLED.get() || event.getSource().is(DamageTypes.GENERIC_KILL) || com.siirio.jemworldbosstiers.api.HostedEncounterApi.isRaid(entity)) return;
        var profile = WorldTierApi.profile(entity).orElse(null);
        if (profile == null) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player
                : entity.getKillCredit() instanceof ServerPlayer player ? player : null;
        if (killer == null || killer instanceof FakePlayer) return;
        var tag = entity.getPersistentData();
        if (tag.hasUUID(COUNTED_ENTITY) && tag.getUUID(COUNTED_ENTITY).equals(entity.getUUID())) return;
        long elapsed = killer.server.overworld().getGameTime() - tag.getLong(DAMAGE_TIME);
        if (!tag.hasUUID(ATTACKER) || !tag.getUUID(ATTACKER).equals(killer.getUUID()) || elapsed < 0 || elapsed > ServerConfig.KILL_CREDIT_TICKS.get()) return;
        pendingDeaths.putIfAbsent(entity.getUUID(), new PendingDeath(entity, killer.getUUID(), killer.getGameProfile().getName(), profile.key(), null));
    }

    @SubscribeEvent
    public void sleeping(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sleep.attempt(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void finishingSleep(SleepFinishedTimeEvent event) { sleep.finishing(event); }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LandmarkNetwork.sync(player);
            MapNetwork.sync(player);
            player.getPersistentData().remove("jem_server_map_request");
            rosterChanged = true;
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) { rosterChanged = true; }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getPersistentData().remove(COUNTED_ENTITY);
        if (event.getEntity() instanceof ServerPlayer player) MapNetwork.sync(player);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        for (PendingDeath pending : pendingDeaths.values()) {
            if (pending.entity().isAlive()) continue;
            ServerData data = ServerData.get(server);
            pending.entity().getPersistentData().putUUID(COUNTED_ENTITY, pending.entity().getUUID());
            if (pending.death() != null) {
                data.addDeath(pending.player(), pending.death());
                var player = server.getPlayerList().getPlayer(pending.player());
                if (player != null) MapNetwork.sync(player);
            }
            else {
                data.recordKill(pending.player(), pending.playerName(), pending.boss());
            }
        }
        pendingDeaths.clear();
        sleep.tick(server);
        if (rosterChanged) {
            sleep.rosterChanged(server);
            rosterChanged = false;
        }
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        pendingDeaths.clear();
        sleep.reset();
        rosterChanged = false;
    }
}
