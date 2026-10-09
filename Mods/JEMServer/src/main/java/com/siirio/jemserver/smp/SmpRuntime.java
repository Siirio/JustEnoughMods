package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import com.siirio.jemworldbosstiers.api.*;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.*;

public final class SmpRuntime {
    private static final DeferredRegister<
                    com.mojang.serialization.Codec<
                            ? extends net.minecraftforge.common.loot.IGlobalLootModifier>>
            LOOT =
                    DeferredRegister.create(
                            ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "jem_server");

    static {
        LOOT.register("resource_rush", () -> RushLoot.CODEC);
    }

    public static void register() {
        var modBus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        SmpEventBlocks.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(
                        net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                        SmpConfig.SPEC,
                        "jem-smp-v2-server.toml");
        LOOT.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, EventRules.SPEC, "jem-events-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, com.siirio.jemserver.smp.fishing.FishingConfig.SPEC, "jem-fishing-server.toml");
        com.siirio.jemserver.smp.fishing.FishingOddsNetwork.register();
        EventNetwork.register();
        SmpNetwork.register();
        Navigation.register();
        CombatNetwork.register();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER) {
            MinecraftForge.EVENT_BUS.register(new SmpRuntime());
            if (net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) registerBossHooks();
        }
    }

    private static void registerBossHooks() {
        MinecraftForge.EVENT_BUS.addListener(
                (com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent event) -> {
                    var server = event.boss().getServer();
                    if (server != null && SmpEnvironment.active(server)) HostedParties.closed(event);
                });
        MinecraftForge.EVENT_BUS.addListener(
                (com.siirio.jemworldbosstiers.event.HostedEncounterAvailableEvent event) ->
                        {
                            if (SmpEnvironment.active(event.actor())) host(event.actor(), event.boss());
                        });
    }

    @SubscribeEvent
    public void starting(net.minecraftforge.event.server.ServerStartingEvent event) {
        if (!SmpEnvironment.active(event.getServer())) return;
        EventHooks.register();
        com.siirio.jemserver.claims.Claims.configureReservation(EventRegions::claimAllowed);
        if (net.minecraftforge.fml.ModList.get().isLoaded("create"))
            com.siirio.jemserver.claims.Claims.configurePlacement(EventHooks::machinePlacement);
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        if (event.getCommandSelection() != Commands.CommandSelection.DEDICATED) return;
        var command =
                Commands.literal("smp")
                        .executes(
                                c -> {
                                    SmpNetwork.open(
                                            c.getSource().getPlayerOrException(), "events", null);
                                    return 1;
                                });
        for (String tab : java.util.List.of("parties", "shops", "events", "profiles", "prizes"))
            command.then(
                    Commands.literal(tab)
                            .executes(
                                    c -> {
                                        SmpNetwork.open(
                                                c.getSource().getPlayerOrException(), tab, null);
                                        return 1;
                                    })
                            .then(
                                    Commands.argument("id", UuidArgument.uuid())
                                            .executes(
                                                    c -> {
                                                        SmpNetwork.open(
                                                                c.getSource()
                                                                        .getPlayerOrException(),
                                                                tab,
                                                                UuidArgument.getUuid(c, "id"));
                                                        return 1;
                                                    })
                                            .then(
                                                    Commands.literal("join")
                                                            .executes(
                                                                    c -> {
                                                                        var p =
                                                                                c.getSource()
                                                                                        .getPlayerOrException();
                                                                        var row =
                                                                                SmpData.get(
                                                                                                p.server)
                                                                                        .find(
                                                                                                "parties",
                                                                                                UuidArgument
                                                                                                        .getUuid(
                                                                                                                c,
                                                                                                                "id"));
                                                                        try {
                                                                            SmpRecords.require(
                                                                                    tab.equals(
                                                                                                    "parties")
                                                                                            && row
                                                                                                    != null,
                                                                                    "unavailable");
                                                                            Parties.join(p, row);
                                                                            Navigation.accepted(p);
                                                                            SmpNetwork.open(
                                                                                    p,
                                                                                    tab,
                                                                                    row.getUUID(
                                                                                            "id"));
                                                                        } catch (
                                                                                IllegalArgumentException
                                                                                        failure) {
                                                                            p.sendSystemMessage(
                                                                                    Component
                                                                                            .translatable(
                                                                                                    "jem.smp.error."
                                                                                                            + failure
                                                                                                                    .getMessage()));
                                                                        }
                                                                        return 1;
                                                                    }))));
        command.then(
                Commands.literal("host")
                        .executes(
                                c -> {
                                    var p = c.getSource().getPlayerOrException();
                                    var bosses =
                                            p.serverLevel()
                                                    .getEntitiesOfClass(
                                                            LivingEntity.class,
                                                            p.getBoundingBox()
                                                                    .inflate(
                                                                            SmpConfig
                                                                                    .ENCOUNTER_RADIUS
                                                                                    .get()),
                                                            b -> BossHostingPrompt.canOffer(p, b));
                                    if (bosses.size() == 1) host(p, bosses.get(0));
                                    else
                                        p.sendSystemMessage(
                                                Component.translatable(
                                                        "jem.smp.error.one_awakened_boss_required"));
                                    return 1;
                                })
                        .then(
                                Commands.argument("boss", UuidArgument.uuid())
                                        .executes(
                                                c -> {
                                                    var p = c.getSource().getPlayerOrException();
                                                    var entity =
                                                            p.serverLevel()
                                                                    .getEntity(
                                                                            UuidArgument.getUuid(
                                                                                    c, "boss"));
                                                    if (entity instanceof LivingEntity boss
                                                            && BossHostingPrompt.canOffer(p, boss))
                                                        host(p, boss);
                                                    else
                                                        p.sendSystemMessage(
                                                                Component.translatable(
                                                                        "jem.smp.error.one_awakened_boss_required"));
                                                    return 1;
                                                })));
        event.getDispatcher().register(command);
    }

    public static void host(ServerPlayer player, LivingEntity boss) {
        if (!SmpEnvironment.active(player)) return;
        var data = SmpData.get(player.server);
        var existing =
                data.all("parties").stream()
                        .filter(
                                row ->
                                        row.hasUUID("bossEntity")
                                                && row.getUUID("bossEntity").equals(boss.getUUID())
                                                && !SmpData.closed(row))
                        .findFirst();
        if (existing.isPresent()) {
            SmpNetwork.open(player, "parties", existing.get().getUUID("id"));
            return;
        }
        try {
            SmpRecords.require(BossHostingPrompt.canOffer(player, boss), "unavailable");
            var args = new com.google.gson.JsonObject();
            args.addProperty("activity", "BOSS");
            args.addProperty("title", boss.getDisplayName().getString());
            Parties.createHosted(player, args);
            var row =
                    data.all("parties").stream()
                            .filter(p -> p.getUUID("owner").equals(player.getUUID()))
                            .max(java.util.Comparator.comparingLong(p -> p.getLong("created")))
                            .orElseThrow();
            SmpRecords.require(HostedEncounterApi.hold(boss), "unavailable");
            row.putUUID("bossEntity", boss.getUUID());
            row.putString("bossType", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString());
            row.putLong("position", boss.blockPosition().asLong());
            data.changed(row);
            SmpNetwork.open(player, "parties", row.getUUID("id"));
        } catch (IllegalArgumentException failure) {
            player.sendSystemMessage(
                    Component.translatable("jem.smp.error." + failure.getMessage()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SmpEnvironment.active(player)
                && event.getTarget() instanceof LivingEntity boss
                && WorldTierApi.profile(boss).isPresent() && BossHostingPrompt.canOffer(player,boss)) {
            host(player, boss);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && SmpEnvironment.active(p)) {
            Profiles.login(p);
            Parties.resetReady(p);
            Navigation.accepted(p);
            var reconnect = EncounterContext.reconnectFor(p);
            if (reconnect != null) new EncounterContext(reconnect).reconnected(p);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && SmpEnvironment.active(p)) {
            var active = EncounterContext.activeFor(p);
            if (active != null) new EncounterContext(active).disconnected(p);
            com.siirio.jemserver.smp.events.HostedBoundary.clear(p.getUUID());
            com.siirio.jemserver.smp.events.BloodMoonVoting.disconnect(p);
            Parties.resetReady(p);
            SmpNetwork.logout(p);
            Navigation.logout(p);
            CombatNetwork.logout(p);
            var profile = Profiles.get(p.server, p.getUUID());
            profile.putInt(
                    "playTicks",
                    p.getStats()
                            .getValue(
                                    net.minecraft.stats.Stats.CUSTOM.get(
                                            net.minecraft.stats.Stats.PLAY_TIME)));
            profile.putInt(
                    "mobsKilled",
                    p.getStats()
                            .getValue(
                                    net.minecraft.stats.Stats.CUSTOM.get(
                                            net.minecraft.stats.Stats.MOB_KILLS)));
            SmpData.get(p.server).changed(profile);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SmpEnvironment.active(event.getServer())) return;
        var server = event.getServer();
        SmpNetwork.tick(server);
        Navigation.tick(server);
        CombatNetwork.tick(server);
        HostedParties.tick(server);
        EventScheduler.tick(server);
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        if (SmpEnvironment.active(event.getServer())) EventHooks.registerSpawnGuard();
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        EventHooks.unregister();
        if (!SmpEnvironment.active(event.getServer())) return;
        EventHooks.unregisterSpawnGuard();
        com.siirio.jemserver.smp.events.HostedBoundary.clear();
        SmpNetwork.clear();
        Navigation.clear();
        CombatNetwork.clear();
        EventScheduler.reset();
    }
}
