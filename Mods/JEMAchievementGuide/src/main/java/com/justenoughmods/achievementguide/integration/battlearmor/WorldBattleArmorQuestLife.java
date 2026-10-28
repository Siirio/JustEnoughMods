package com.justenoughmods.achievementguide.integration.battlearmor;
import net.minecraftforge.eventbus.api.Event;import net.minecraftforge.event.entity.player.PlayerEvent;import net.minecraftforge.event.entity.living.LivingDeathEvent;import net.minecraftforge.event.server.ServerStoppingEvent;import com.github.L_Ender.lionfishapi.server.event.StandOnFluidEvent;
public final class WorldBattleArmorQuestLife {
 public static void posted(Event event){WorldBattleArmorQuestDamage.posted(event);if(event instanceof StandOnFluidEvent fluid)WorldBattleArmorQuestMotion.posted(fluid);
  if(event instanceof PlayerEvent.PlayerLoggedOutEvent player&&player.getEntity() instanceof net.minecraft.server.level.ServerPlayer)clear(player.getEntity().getUUID());
  if(event instanceof PlayerEvent.PlayerChangedDimensionEvent player&&player.getEntity() instanceof net.minecraft.server.level.ServerPlayer)clear(player.getEntity().getUUID());
  if(event instanceof LivingDeathEvent death&&!death.isCanceled()&&death.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)clear(death.getEntity().getUUID());
  if(event instanceof ServerStoppingEvent){WorldBattleArmorQuestDamage.clear();WorldBattleArmorQuestMotion.clear();WorldBattleArmorQuestCurios.clear();WorldBattleArmorQuestFusion.clear();}
 }
 private static void clear(java.util.UUID actor){WorldBattleArmorQuestMotion.clear(actor);WorldBattleArmorQuestCurios.clear(actor);}
}
