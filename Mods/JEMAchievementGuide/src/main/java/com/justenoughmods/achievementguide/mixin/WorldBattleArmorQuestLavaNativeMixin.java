package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import com.github.L_Ender.lionfishapi.server.event.StandOnFluidEvent;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.github.L_Ender.cataclysm.event.ServerEventHandler",remap=false) public abstract class WorldBattleArmorQuestLavaNativeMixin {
 @Inject(method="StandOnFluidEventEvent",at=@At("HEAD"),remap=false) private void jem$before(StandOnFluidEvent event,CallbackInfo ci){WorldBattleArmorQuestMotion.fluidBefore(event);}
 @Redirect(method="StandOnFluidEventEvent",at=@At(value="INVOKE",target="Lcom/github/L_Ender/lionfishapi/server/event/StandOnFluidEvent;setCanceled(Z)V",remap=false),remap=false) private void jem$native(StandOnFluidEvent event,boolean canceled){boolean before=event.isCanceled();event.setCanceled(canceled);WorldBattleArmorQuestMotion.fluidNative(event,before);}
 @Inject(method="StandOnFluidEventEvent",at=@At("RETURN"),remap=false) private void jem$after(StandOnFluidEvent event,CallbackInfo ci){WorldBattleArmorQuestMotion.fluidAfter(event);}
}
