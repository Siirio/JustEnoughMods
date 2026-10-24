package com.siirio.jemfood.mixin;

import com.siirio.jemfood.PlacedConsumption;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Inject(method = "eat(IF)V", at = @At("RETURN"))
    private void nativeHunger(int food, float saturation, CallbackInfo callback) { PlacedConsumption.hunger((FoodData) (Object) this, food); }
    @Inject(method = "setFoodLevel", at = @At("HEAD"))
    private void nativeFoodLevel(int food, CallbackInfo callback) {
        PlacedConsumption.hunger((FoodData) (Object) this, food - ((FoodData) (Object) this).getFoodLevel());
    }
}
