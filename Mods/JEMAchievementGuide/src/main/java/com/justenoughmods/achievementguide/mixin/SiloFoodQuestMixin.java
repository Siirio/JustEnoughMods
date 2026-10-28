package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.SiloFoodProof;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.SiloBlockEntity;
import net.satisfy.farm_and_charm.core.recipe.SiloRecipe;
import net.satisfy.farm_and_charm.core.world.ImplementedInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SiloBlockEntity.class, remap = false)
public abstract class SiloFoodQuestMixin implements ImplementedInventory {
    @Unique private boolean jem$dryWheat;
    @Override public void setItem(int slot, ItemStack stack) {
        SiloFoodProof.invalidate((BlockEntity)(Object)this, slot);
        ImplementedInventory.super.m_6836_(slot, stack);
    }
    @Override public ItemStack removeItem(int slot, int count) {
        SiloFoodProof.invalidate((BlockEntity)(Object)this, slot);
        return ImplementedInventory.super.m_7407_(slot, count);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        SiloFoodProof.invalidate((BlockEntity)(Object)this, slot);
        return ImplementedInventory.super.m_8016_(slot);
    }
    @Override public void clearContent() {
        ((BlockEntity)(Object)this).getPersistentData().remove(SiloFoodProof.KEY);
        ImplementedInventory.super.m_6211_();
    }
    @Inject(method = "dry", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$dryWheat = false; }
    @Redirect(method = "dry", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/recipe/SiloRecipe;m_8043_(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$result(SiloRecipe recipe, RegistryAccess registries) {
        ItemStack result = recipe.getResultItem(registries);
        jem$dryWheat = recipe.getId().toString().equals("farm_and_charm:drying/dried_wheat")
                && FoodOutputProof.item(result, "brewery:dried_wheat");
        return result;
    }
    @Redirect(method = "dry", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/entity/SiloBlockEntity;m_6836_(ILnet/minecraft/world/item/ItemStack;)V"))
    private void jem$commit(SiloBlockEntity owner, int slot, ItemStack output) {
        owner.setItem(slot, output);
        if (jem$dryWheat && ItemStack.matches(owner.getItem(slot), output)) SiloFoodProof.certify(owner, slot, output);
        jem$dryWheat = false;
    }
    @Redirect(method = "tryDropFinish", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/entity/SiloBlockEntity;dropStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)V"))
    private void jem$eject(Level level, double x, double y, double z, ItemStack output, Direction direction) {
        SiloBlockEntity owner = (SiloBlockEntity)(Object)this;
        for (int slot = 81; slot < 162; slot++) {
            if (owner.getItem(slot) == output) { SiloFoodProof.prepare(owner, slot, output); break; }
        }
        try { SiloBlockEntity.dropStack(level, x, y, z, output, direction); }
        finally { SiloFoodProof.clean(output); }
    }
    @Redirect(method = "dropStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$drop(Level level, Entity entity) {
        SiloFoodProof.beforeInsertion(entity);
        return level.addFreshEntity(entity);
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { SiloFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { SiloFoodProof.load((BlockEntity)(Object)this, tag); }
}
