package com.justenoughmods.achievementguide.integration.worldritual;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="jem_advancements",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldRitualQuestLifecycle {
    private WorldRitualQuestLifecycle(){}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){if(ModList.get().isLoaded("luminous_nether")){WorldRitualQuestPotter.clear();WorldRitualQuestEctoplasm.clear();WorldRitualQuestRitual.clear();WorldRitualQuestPickup.clear();}if(ModList.get().isLoaded("cnc"))WorldRitualQuestStun.clear();}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){forget(event);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){forget(event);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(ModList.get().isLoaded("cnc"))WorldRitualQuestStun.forget(event.getEntity().getUUID());}
    private static void forget(PlayerEvent event){if(ModList.get().isLoaded("luminous_nether"))WorldRitualQuestRitual.forget(event.getEntity().getUUID());if(ModList.get().isLoaded("cnc"))WorldRitualQuestStun.forget(event.getEntity().getUUID());}
}
