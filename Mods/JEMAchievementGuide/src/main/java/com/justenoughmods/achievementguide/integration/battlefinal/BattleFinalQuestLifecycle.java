package com.justenoughmods.achievementguide.integration.battlefinal;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="jem_advancements",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BattleFinalQuestLifecycle {
    private BattleFinalQuestLifecycle(){}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){BattleFinalQuestLife.clear();if(ModList.get().isLoaded("companions")&&ModList.get().isLoaded("knightlib"))com.justenoughmods.achievementguide.integration.companions.CompanionQuestSpell.clear();if(ModList.get().isLoaded("unearthed_journey"))com.justenoughmods.achievementguide.integration.prehistoric.WorldPrehistoricQuestDart.clear();if(ModList.get().isLoaded("luminous_beasts"))BattleFinalQuestRelics.clear();if(ModList.get().isLoaded("epicfight")){BattleFinalQuestPassive.clear();BattleFinalQuestMeteor.clear();}if(ModList.get().isLoaded("efn"))BattleFinalQuestNightfall.clear();}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){forget(event);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){forget(event);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){forget(event);}
    private static void forget(PlayerEvent event){if(ModList.get().isLoaded("epicfight"))BattleFinalQuestMeteor.forget(event.getEntity().getUUID());if(ModList.get().isLoaded("efn"))BattleFinalQuestNightfall.forget(event.getEntity().getUUID());}
}
