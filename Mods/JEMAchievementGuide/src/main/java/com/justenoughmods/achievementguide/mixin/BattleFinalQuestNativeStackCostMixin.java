package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.hm.efn.comboevents.TimeEvents.class,remap=false)
public abstract class BattleFinalQuestNativeStackCostMixin {
    @Inject(method="lambda$timeConsumeStack$6",at=@At("HEAD"))private static void jem$before(int amount,yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?> patch,CallbackInfo ci){BattleFinalQuestNightfall.costBegin(patch);}
    @Inject(method="lambda$timeConsumeStack$6",at=@At("RETURN"))private static void jem$after(int amount,yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?> patch,CallbackInfo ci){BattleFinalQuestNightfall.costEnd();}
}
