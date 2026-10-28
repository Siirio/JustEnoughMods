package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.PrimordialFoodProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PrimordialMealQuestMixin {
    @Unique private PrimordialFoodProof.Run jem$primordialMeal;
    @Inject(method = "eat", at = @At("HEAD"))
    private void jem$beforeMeal(Level level, ItemStack meal, CallbackInfoReturnable<ItemStack> callback) {
        jem$primordialMeal = (Object) this instanceof ServerPlayer actor ? PrimordialFoodProof.begin(actor, meal) : null;
    }
    @Inject(method = "eat", at = @At("RETURN"))
    private void jem$afterMeal(Level level, ItemStack meal, CallbackInfoReturnable<ItemStack> callback) {
        var run = jem$primordialMeal;
        jem$primordialMeal = null;
        if (run != null) PrimordialFoodProof.finish(run);
    }
}
