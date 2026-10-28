package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native stockpot already persists exact recipe ID, result, completion status and servings. */
@Mixin(value = StockpotBlockEntity.class, remap = false)
public abstract class CookeryStockpotFoodQuestMixin {
    @Shadow private ResourceLocation recipeId;
    @Unique private ItemStack jem$serving = ItemStack.EMPTY;
    @Unique private int jem$held;
    @Unique private int jem$portions;
    @Inject(method = "takeOutProduct", at = @At("HEAD"))
    private void jem$before(Level level, LivingEntity actor, ItemStack carrier, CallbackInfoReturnable<Boolean> callback) {
        StockpotBlockEntity owner = (StockpotBlockEntity)(Object)this;
        jem$serving = ItemStack.EMPTY;
        if (actor instanceof ServerPlayer player && owner.getStatus() == 3 && owner.getTakeoutCount() > 0
                && recipeId != null && recipeId.toString().matches("kaleidoscope_cookery:(flex_stockpot|stockpot)/beef_noodle")
                && FoodOutputProof.item(owner.getResult(), "kaleidoscope_cookery:beef_noodle")) {
            jem$serving = owner.getResult().copyWithCount(1);
            jem$held = FoodOutputProof.held(player, player.containerMenu, jem$serving);
            jem$portions = owner.getTakeoutCount();
        }
    }
    @Inject(method = "takeOutProduct", at = @At("RETURN"))
    private void jem$after(Level level, LivingEntity actor, ItemStack carrier, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && actor instanceof ServerPlayer player && !jem$serving.isEmpty()
                && ((StockpotBlockEntity)(Object)this).getTakeoutCount() == jem$portions - 1
                && FoodOutputProof.held(player, player.containerMenu, jem$serving) > jem$held)
            QuestActionTrigger.INSTANCE.emit(player, "food/stockpot_noodles");
        jem$serving = ItemStack.EMPTY;
    }
}
