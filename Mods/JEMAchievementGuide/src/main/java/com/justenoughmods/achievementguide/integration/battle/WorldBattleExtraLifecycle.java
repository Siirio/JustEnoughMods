package com.justenoughmods.achievementguide.integration.battle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class WorldBattleExtraLifecycle {
    private WorldBattleExtraLifecycle() {}
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        if (ModList.get().isLoaded("cataclysm")) { BattleQuestSmithing.clear(); WorldBattleExtraProof.clear(); }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (ModList.get().isLoaded("cataclysm")) WorldBattleExtraProof.clear();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if(ModList.get().isLoaded("cataclysm")&&event.getEntity() instanceof ServerPlayer actor)WorldBattleExtraMovement.suspend(actor); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { forget(event); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (ModList.get().isLoaded("cataclysm") && event.getOriginal() instanceof ServerPlayer actor) WorldBattleExtraMovement.clear(actor);
        forget(event);
    }
    private static void forget(PlayerEvent event) {
        if (ModList.get().isLoaded("cataclysm") && event.getEntity() instanceof ServerPlayer actor) WorldBattleExtraMovement.clear(actor);
    }
}
