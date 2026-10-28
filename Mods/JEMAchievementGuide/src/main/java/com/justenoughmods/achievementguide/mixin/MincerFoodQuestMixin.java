package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.block.entity.MincerBlockEntity;
import net.satisfy.farm_and_charm.core.recipe.MincerRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native mincer output is ejected next tick. Its receipt follows that exact spawned entity. */
@Mixin(value = MincerBlockEntity.class, remap = false)
public abstract class MincerFoodQuestMixin {
    @Unique private String jem$recipe = "";
    @Unique private ItemStack jem$result = ItemStack.EMPTY;
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;)V", at = @At("HEAD"))
    private void jem$begin(Level level, BlockPos pos, BlockState state, MincerBlockEntity owner, CallbackInfo callback) { jem$recipe = ""; jem$result = ItemStack.EMPTY; }
    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/recipe/MincerRecipe;m_8043_(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeResult(MincerRecipe recipe, RegistryAccess registries) {
        ItemStack result = recipe.getResultItem(registries);
        jem$recipe = recipe.getId().toString();
        jem$result = result.copy();
        return result;
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;)V", at = @At("RETURN"))
    private void jem$committed(Level level, BlockPos pos, BlockState state, MincerBlockEntity owner, CallbackInfo callback) {
        if (jem$recipe.equals("farm_and_charm:mincer/flour") && FoodOutputProof.item(jem$result, "farm_and_charm:flour")
                && ItemStack.matches(jem$result, owner.getItem(1))) FoodOutputProof.certify(owner, 1, jem$recipe, "food_mincer_flour", jem$result);
        jem$recipe = ""; jem$result = ItemStack.EMPTY;
    }
    @Redirect(method = "dropItemsInOutputSlot", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/entity/MincerBlockEntity;spawnItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Position;)V"))
    private void jem$nativeEjection(Level level, ItemStack output, int speed, Direction direction, Position pos) {
        MincerBlockEntity.spawnItem(level, FoodDroppedProof.outgoing((BlockEntity)(Object)this, output), speed, direction, pos);
    }
    @Redirect(method = "spawnItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$attachDropReceipt(Level level, Entity entity) {
        FoodDroppedProof.beforeWorldInsertion(entity);
        return level.addFreshEntity(entity);
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
