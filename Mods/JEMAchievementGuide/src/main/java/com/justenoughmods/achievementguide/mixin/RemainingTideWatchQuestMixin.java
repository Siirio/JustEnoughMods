package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import com.li64.tide.data.FreezableMob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.li64.tide.registries.items.EnchantedPocketWatchItem", remap = false)
public abstract class RemainingTideWatchQuestMixin {
    @Redirect(method = "m_6880_", at = @At(value = "INVOKE", target = "Lcom/li64/tide/data/FreezableMob;tide$setFrozen(Z)V"))
    private void jem$nativeFreeze(FreezableMob target, boolean freeze, ItemStack watch, Player player, LivingEntity creature, InteractionHand hand) {
        boolean before = target.tide$isFrozen();
        target.tide$setFrozen(freeze);
        var actor = RemainingFoodActors.real(player);
        if (actor != null && creature == target && !before && freeze && target.tide$isFrozen()) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_catch_toolkit");
        }
    }
}
