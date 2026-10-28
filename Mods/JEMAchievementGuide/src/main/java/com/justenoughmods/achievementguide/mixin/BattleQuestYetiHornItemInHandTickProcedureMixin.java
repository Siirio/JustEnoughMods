package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestRelics;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.mcreator.luminousbeasts.procedures.YetiHornItemInHandTickProcedure.class,remap=false)
public abstract class BattleQuestYetiHornItemInHandTickProcedureMixin {
    @Inject(method="execute",at=@At("HEAD")) private static void jem$before(LevelAccessor world,double x,double y,double z,Entity actor,CallbackInfo ci){BattleQuestRelics.begin(actor,"yeti",actor instanceof LivingEntity living?living.getOffhandItem():ItemStack.EMPTY);}
    @Inject(method="execute",at=@At("RETURN")) private static void jem$after(LevelAccessor world,double x,double y,double z,Entity actor,CallbackInfo ci){BattleQuestRelics.end();}
}
