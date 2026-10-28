package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ShawarmaSpitBlockEntity;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.CookeryFoodProof;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShawarmaSpitBlockEntity.class, remap = false)
public abstract class CookerySpitFoodQuestMixin {
    @Unique private ItemStack jem$serving = ItemStack.EMPTY;
    @Unique private int jem$held;
    @Inject(method = "lambda$onPutCookingItem$0", at = @At("RETURN"))
    private void jem$recipe(ItemStack input, SimpleContainer inventory, Level level, CampfireCookingRecipe recipe, CallbackInfoReturnable<Boolean> callback) {
        ShawarmaSpitBlockEntity owner = (ShawarmaSpitBlockEntity)(Object)this;
        if (Boolean.TRUE.equals(callback.getReturnValue()) && !owner.cookingItem.isEmpty() && owner.cookedItem.isEdible())
            CookeryFoodProof.certify(owner, recipe.getId().toString(), owner.cookedItem);
    }
    @Inject(method = "onTakeCookedItem", at = @At("HEAD"))
    private void jem$before(Level level, LivingEntity actor, CallbackInfoReturnable<Boolean> callback) {
        ShawarmaSpitBlockEntity owner = (ShawarmaSpitBlockEntity)(Object)this;
        jem$serving = ItemStack.EMPTY;
        if (actor instanceof ServerPlayer player && owner.cookTime <= 0 && owner.cookedItem.isEdible()
                && !CookeryFoodProof.recipe(owner, owner.cookedItem).isEmpty()) {
            jem$serving = owner.cookedItem.copy();
            jem$held = FoodOutputProof.held(player, player.containerMenu, jem$serving);
        }
    }
    @Inject(method = "onTakeCookedItem", at = @At("RETURN"))
    private void jem$after(Level level, LivingEntity actor, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            CookeryFoodProof.clear((BlockEntity)(Object)this);
            if (actor instanceof ServerPlayer player && !jem$serving.isEmpty()
                    && ((ShawarmaSpitBlockEntity)(Object)this).cookedItem.isEmpty()
                    && FoodOutputProof.held(player, player.containerMenu, jem$serving) > jem$held)
                QuestActionTrigger.INSTANCE.emit(player, "food/spit_portion");
        }
        jem$serving = ItemStack.EMPTY;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.load((BlockEntity)(Object)this, tag); }
}
