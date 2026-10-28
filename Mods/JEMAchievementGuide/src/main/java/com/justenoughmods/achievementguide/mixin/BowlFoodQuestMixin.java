package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.block.entity.CraftingBowlBlockEntity;
import net.satisfy.farm_and_charm.core.recipe.CraftingBowlRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native ingredient consumption precedes getResultItem, then the completed dough enters output four. */
@Mixin(value = CraftingBowlBlockEntity.class, remap = false)
public abstract class BowlFoodQuestMixin {
    @Unique private String jem$recipe = "";
    @Unique private ItemStack jem$result = ItemStack.EMPTY;
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/CraftingBowlBlockEntity;)V", at = @At("HEAD"))
    private void jem$begin(Level level, BlockPos pos, BlockState state, CraftingBowlBlockEntity owner, CallbackInfo callback) { jem$recipe = ""; jem$result = ItemStack.EMPTY; }
    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/CraftingBowlBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/recipe/CraftingBowlRecipe;m_8043_(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeResult(CraftingBowlRecipe recipe, RegistryAccess registries) {
        ItemStack result = recipe.getResultItem(registries);
        jem$recipe = recipe.getId().toString();
        jem$result = result.copyWithCount(recipe.getOutputCount());
        return result;
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/CraftingBowlBlockEntity;)V", at = @At("RETURN"))
    private void jem$committed(Level level, BlockPos pos, BlockState state, CraftingBowlBlockEntity owner, CallbackInfo callback) {
        if (jem$recipe.equals("farm_and_charm:crafting_bowl/dough") && FoodOutputProof.item(jem$result, "farm_and_charm:dough")
                && ItemStack.matches(jem$result, owner.getItem(4))) FoodOutputProof.certify(owner, 4, jem$recipe, "food_bowl_dough", jem$result);
        jem$recipe = ""; jem$result = ItemStack.EMPTY;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
