package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.CookeryFoodProof;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PotBlockEntity.class, remap = false)
public abstract class CookeryPotFoodQuestMixin {
    @Unique private ItemStack jem$serving = ItemStack.EMPTY;
    @Unique private int jem$held;
    @Inject(method = {"startCooking", "reset"}, at = @At("HEAD"))
    private void jem$invalidate(CallbackInfo callback) { CookeryFoodProof.clear((BlockEntity)(Object)this); }
    @Inject(method = "applyRecipe", at = @At("RETURN"))
    private void jem$recipe(Level level, SimpleContainer inventory, PotRecipe recipe, CallbackInfo callback) {
        PotBlockEntity owner = (PotBlockEntity)(Object)this;
        String id = recipe.id().toString();
        if (id.matches("kaleidoscope_cookery:pot/egg_to_fried_egg_[1-9]") && FoodOutputProof.item(owner.getResult(), "kaleidoscope_cookery:fried_egg"))
            CookeryFoodProof.certify(owner, id, owner.getResult());
    }
    @Inject(method = "takeOutProduct", at = @At("HEAD"))
    private void jem$before(Level level, LivingEntity actor, ItemStack carrier, CallbackInfoReturnable<Boolean> callback) {
        PotBlockEntity owner = (PotBlockEntity)(Object)this;
        jem$serving = ItemStack.EMPTY;
        if (actor instanceof ServerPlayer player && owner.getStatus() == 2
                && CookeryFoodProof.recipe(owner, owner.getResult()).matches("kaleidoscope_cookery:pot/egg_to_fried_egg_[1-9]")) {
            jem$serving = owner.getResult().copy();
            jem$held = FoodOutputProof.held(player, player.containerMenu, jem$serving);
        }
    }
    @Inject(method = "takeOutProduct", at = @At("RETURN"))
    private void jem$after(Level level, LivingEntity actor, ItemStack carrier, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && actor instanceof ServerPlayer player && !jem$serving.isEmpty()
                && ((PotBlockEntity)(Object)this).getStatus() == 0
                && FoodOutputProof.held(player, player.containerMenu, jem$serving) > jem$held)
            QuestActionTrigger.INSTANCE.emit(player, "food/wok_egg");
        jem$serving = ItemStack.EMPTY;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.load((BlockEntity)(Object)this, tag); }
}
