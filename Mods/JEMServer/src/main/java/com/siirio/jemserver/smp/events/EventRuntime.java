package com.siirio.jemserver.smp.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.BiConsumer;

public final class EventRuntime {
    private static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "jem_server");

    static {
        LOOT.register("resource_rush", () -> RushLoot.CODEC);
    }

    public static void register(BiConsumer<ServerPlayer, EventChoice> entryActions, boolean dedicated) {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        SmpEventBlocks.register(bus);
        LOOT.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EventRules.SPEC, "jem-events-server.toml");
        EventNetwork.register(entryActions);
    }

    public static boolean enabled(MinecraftServer server) {
        return server != null && server.isDedicatedServer() && EventRules.ENABLED.get();
    }

    public static boolean available(MinecraftServer server, net.minecraft.nbt.CompoundTag row) {
        return row != null && com.siirio.jemserver.smp.SmpEnvironment.active(server)
                && (enabled(server) || ordinaryRaid(row));
    }

    public static boolean ordinaryRaid(net.minecraft.nbt.CompoundTag row) {
        return row != null && row.getBoolean("ordinaryRaid") && row.getString("activity").equals("BOSS_RAID");
    }

    public static void starting(MinecraftServer server) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(server)) return;
        EventHooks.register();
    }

    public static void started(MinecraftServer server) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(server)) return;
        EventHooks.registerSpawnGuard();
    }

    public static void login(ServerPlayer player) {
        var event = EncounterContext.reconnectFor(player);
        if (available(player.getServer(), event)) new EncounterContext(event).reconnected(player);
        if (enabled(player.getServer())) com.siirio.jemserver.smp.events.calendar.CalendarEventScheduler.login(player);
    }

    public static void logout(ServerPlayer player) {
        var event = EncounterContext.activeFor(player);
        if (available(player.getServer(), event)) new EncounterContext(event).disconnected(player);
        if (enabled(player.getServer())) BloodMoonVoting.disconnect(player);
        EventTravel.cancel(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        EventScheduler.tick(server);
        if (enabled(server)) com.siirio.jemserver.smp.events.calendar.CalendarEventScheduler.tick(server);
    }

    public static void stopped() {
        EventHooks.unregister();
        EventScheduler.reset();
        com.siirio.jemserver.smp.events.calendar.CalendarEventScheduler.reset();
    }

    private EventRuntime() {}
}
