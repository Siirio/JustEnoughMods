package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class RemainingPreparedMealUseMixin {
    @Unique private ItemStack jem$meal;
    @Unique private int jem$count;
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void jem$begin(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) {
        jem$meal = null;
        if (entity instanceof ServerPlayer actor && RemainingFoodActors.real(actor) != null) {
            ItemStack meal = (ItemStack)(Object)this; jem$meal = meal.copy(); jem$count = meal.getCount();
        }
    }
    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void jem$finish(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack before = jem$meal; jem$meal = null;
        if (before != null && entity instanceof ServerPlayer actor) NativePreparedMealProof.eaten(actor, before, (ItemStack)(Object)this, jem$count);
    }
}
