package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=yesman.epicfight.events.EntityEvents.class,remap=false)
public abstract class BattleFinalQuestNativeKillMixin {
    @Inject(method="deathEvent",at=@At("HEAD"))private static void jem$begin(net.minecraftforge.event.entity.living.LivingDeathEvent event,CallbackInfo ci){BattleFinalQuestPassive.eventBegin(event);}
    @Inject(method="deathEvent",at=@At("RETURN"))private static void jem$end(net.minecraftforge.event.entity.living.LivingDeathEvent event,CallbackInfo ci){BattleFinalQuestPassive.eventEnd();}
}
