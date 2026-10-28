package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import com.justenoughmods.achievementguide.integration.worldmisc.WorldHungerGuardProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NativePreparedMealProof.class, remap = false)
public abstract class WorldHungerMealQuestMixin {
    @Inject(method = "eaten", at = @At("HEAD"))
    private static void jem$actualMeal(ServerPlayer actor, ItemStack before, ItemStack actual, int initialCount, CallbackInfo ci) { WorldHungerGuardProof.eaten(actor, before, actual, initialCount); }
}
