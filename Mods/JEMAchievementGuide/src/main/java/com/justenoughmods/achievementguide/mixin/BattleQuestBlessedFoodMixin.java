package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestRelics;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class BattleQuestBlessedFoodMixin {
    @Inject(method="finishUsingItem",at=@At("HEAD")) private void jem$before(ItemStack stack,Level level,LivingEntity actor,CallbackInfoReturnable<ItemStack> ci){BattleQuestRelics.begin(actor,"blessed",stack);}
    @Inject(method="finishUsingItem",at=@At("RETURN")) private void jem$after(ItemStack stack,Level level,LivingEntity actor,CallbackInfoReturnable<ItemStack> ci){BattleQuestRelics.end();}
}
