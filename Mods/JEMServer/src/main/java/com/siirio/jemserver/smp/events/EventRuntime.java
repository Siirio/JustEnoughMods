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

    public static void register(BiConsumer<ServerPlayer, EventChoice> entryActions) {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        SmpEventBlocks.register(bus);
        LOOT.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EventRules.SPEC, "jem-events-server.toml");
        EventNetwork.register(entryActions);
    }

    public static void starting() {
        EventHooks.register();
    }

    public static void started() {
        EventHooks.registerSpawnGuard();
    }

    public static void login(ServerPlayer player) {
        var event = EncounterContext.reconnectFor(player);
        if (event != null) new EncounterContext(event).reconnected(player);
    }

    public static void logout(ServerPlayer player) {
        var event = EncounterContext.activeFor(player);
        if (event != null) new EncounterContext(event).disconnected(player);
        BloodMoonVoting.disconnect(player);
        EventTravel.cancel(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        EventScheduler.tick(server);
    }

    public static void stopped() {
        EventHooks.unregister();
        EventScheduler.reset();
    }

    private EventRuntime() {}
}
