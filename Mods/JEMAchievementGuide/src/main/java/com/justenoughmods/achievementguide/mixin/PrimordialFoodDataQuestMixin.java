package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.PrimordialFoodProof;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class PrimordialFoodDataQuestMixin {
    @Unique private PrimordialFoodProof.Mutation jem$nativeArmorMeal;
    @Inject(method = "eat(IF)V", at = @At("HEAD"))
    private void jem$beforeBoostedFood(int food, float saturation, CallbackInfo callback) {
        jem$nativeArmorMeal = PrimordialFoodProof.beforeMutation((FoodData) (Object) this, food, saturation);
    }
    @Inject(method = "eat(IF)V", at = @At("RETURN"))
    private void jem$afterBoostedFood(int food, float saturation, CallbackInfo callback) {
        var before = jem$nativeArmorMeal;
        jem$nativeArmorMeal = null;
        PrimordialFoodProof.afterMutation((FoodData) (Object) this, before);
    }
}
