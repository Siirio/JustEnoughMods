package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestNightfall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
@Mixin(value=com.hm.efn.comboevents.TimeEvents.class,remap=false)
public abstract class BattleQuestNightfallCostMixin {
    @Unique private static final ThreadLocal<Integer> jem$before=new ThreadLocal<>();
    @Inject(method="lambda$timeConsumeStack$6",at=@At("HEAD")) private static void jem$before(int amount,PlayerPatch<?> patch,CallbackInfo ci) { jem$before.set(BattleQuestNightfall.stack(patch)); }
    @Inject(method="lambda$timeConsumeStack$6",at=@At("RETURN")) private static void jem$after(int amount,PlayerPatch<?> patch,CallbackInfo ci) { Integer before=jem$before.get();jem$before.remove();if(before!=null) BattleQuestNightfall.paid(patch,before); }
}
