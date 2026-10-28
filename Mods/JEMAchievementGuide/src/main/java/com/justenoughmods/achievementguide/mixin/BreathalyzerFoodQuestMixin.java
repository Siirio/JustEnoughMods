package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.satisfy.brewery.core.effect.alcohol.AlcoholPlayer;
import net.satisfy.brewery.core.item.Breathalyzer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Breathalyzer.class, remap = false)
public abstract class BreathalyzerFoodQuestMixin {
    @Inject(method = "addNbt", at = @At("RETURN"))
    private void jem$measured(LivingEntity actor, CallbackInfo callback) {
        if (!(actor instanceof ServerPlayer player) || !(actor instanceof AlcoholPlayer) || !actor.isUsingItem()) return;
        ItemStack tool = actor.getItemInHand(actor.getUsedItemHand());
        if (tool.getItem() != (Object)this || actor.getUseItem() != tool || !tool.hasTag()) return;
        String result = tool.getTag().getString("brewery.drunkenness");
        if (result.equals("EASY") || result.equals("WARNING") || result.equals("DANGER"))
            QuestActionTrigger.INSTANCE.emit(player, "food_breathalyzer_measured");
    }
}
