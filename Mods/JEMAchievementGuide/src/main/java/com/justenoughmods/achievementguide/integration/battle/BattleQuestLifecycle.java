package com.justenoughmods.achievementguide.integration.battle;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="jem_advancements",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BattleQuestLifecycle {
    private BattleQuestLifecycle() {}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){BattleQuestArtifacts.clear();BattleQuestSmithing.clear();BattleQuestRelics.clear();BattleQuestPhoenix.clear();BattleQuestPit.clear();if(ModList.get().isLoaded("epicfight"))BattleQuestEpic.clear();if(ModList.get().isLoaded("efn")){BattleQuestNightfall.clear();BattleQuestDamage.clear();}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){forget(event);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){forget(event);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){forget(event);}
    private static void forget(PlayerEvent event){if(ModList.get().isLoaded("epicfight"))BattleQuestEpic.forget(event.getEntity().getUUID());if(ModList.get().isLoaded("efn"))BattleQuestNightfall.forget(event.getEntity().getUUID());}
}
