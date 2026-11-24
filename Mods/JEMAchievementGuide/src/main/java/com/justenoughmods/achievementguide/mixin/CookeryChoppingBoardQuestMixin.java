package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.justenoughmods.achievementguide.integration.food.ChoppingBoardFoodDrop;
import com.justenoughmods.achievementguide.integration.food.CookeryFoodProof;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChoppingBoardBlockEntity.class, remap = false)
public abstract class CookeryChoppingBoardQuestMixin {
    @Inject(method = "onPutItem", at = @At("RETURN"))
    private void jem$accepted(Level level, LivingEntity actor, ItemStack input,
                              CallbackInfoReturnable<Boolean> callback) {
        ChoppingBoardBlockEntity owner = (ChoppingBoardBlockEntity)(Object)this;
        if (!callback.getReturnValue()) {
            CookeryFoodProof.clear(owner);
            return;
        }
        level.getRecipeManager().getRecipeFor(ModRecipes.CHOPPING_BOARD_RECIPE,
                        new SimpleContainer(owner.getCurrentCutStack()), level)
                .filter(recipe -> !recipe.getResults().isEmpty())
                .ifPresentOrElse(recipe -> CookeryFoodProof.certify(owner, recipe.getId().toString(),
                                recipe.getResults().get(0)),
                        () -> CookeryFoodProof.clear(owner));
    }
    @Redirect(method = "onCutItem", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/ChoppingBoardBlockEntity;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$completed(Level level, BlockPos pos, ItemStack result, Level nativeLevel, LivingEntity actor, ItemStack tool) {
        ItemStack outgoing = actor instanceof ServerPlayer player && tool.is(TagMod.KITCHEN_KNIFE)
                ? ChoppingBoardFoodDrop.outgoing((BlockEntity)(Object)this, result, player) : result;
        ChoppingBoardBlockEntity.popResource(level, pos, outgoing);
    }
    @Redirect(method = "popResource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$drop(Level level, Entity entity) {
        ChoppingBoardFoodDrop.beforeInsertion(entity);
        return level.addFreshEntity(entity);
    }
    @Inject(method = "resetBoardData", at = @At("HEAD"))
    private void jem$reset(CallbackInfo callback) { CookeryFoodProof.clear((BlockEntity)(Object)this); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.load((BlockEntity)(Object)this, tag); }
}
