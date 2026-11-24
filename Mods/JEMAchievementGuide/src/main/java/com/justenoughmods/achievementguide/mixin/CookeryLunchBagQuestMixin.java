package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TransmutationLunchBagItem;
import com.justenoughmods.achievementguide.integration.food.LunchBagFoodProof;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the exact native 1.7.0 sequential-consumption transaction. */
@Mixin(value = TransmutationLunchBagItem.class, remap = false)
public abstract class CookeryLunchBagQuestMixin {
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack bag, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        LunchBagFoodProof.begin(bag, level, actor);
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$finished(ItemStack bag, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        LunchBagFoodProof.finish(bag, level, actor, callback.getReturnValue());
    }
}
