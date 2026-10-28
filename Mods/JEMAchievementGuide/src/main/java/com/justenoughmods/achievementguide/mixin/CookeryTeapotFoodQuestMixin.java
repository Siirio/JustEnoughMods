package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.TeapotContainer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.TeapotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import com.justenoughmods.achievementguide.integration.food.CookeryFoodProof;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TeapotBlockEntity.class, remap = false)
public abstract class CookeryTeapotFoodQuestMixin {
    @Unique private String jem$recipe = "";
    @Unique private ItemStack jem$assembled = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$recipe = ""; jem$assembled = ItemStack.EMPTY; }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/recipe/TeapotRecipe;assemble(Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/container/TeapotContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeRecipe(TeapotRecipe recipe, TeapotContainer input, RegistryAccess registries) {
        ItemStack result = recipe.assemble(input, registries);
        jem$recipe = recipe.id().toString();
        jem$assembled = result.copy();
        return result;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$committed(CallbackInfo callback) {
        TeapotBlockEntity owner = (TeapotBlockEntity)(Object)this;
        if (owner.getStatus() == 0) CookeryFoodProof.clear(owner);
        else if (!jem$recipe.isEmpty() && owner.getStatus() == 1
                && ItemStack.matches(jem$assembled, owner.getResult()))
            CookeryFoodProof.certify(owner, jem$recipe, owner.getResult());
        jem$recipe = "";
        jem$assembled = ItemStack.EMPTY;
    }

    @Inject(method = "getDrops", at = @At("RETURN"))
    private void jem$portableProof(CallbackInfoReturnable<List<ItemStack>> callback) {
        TeapotBlockEntity owner = (TeapotBlockEntity)(Object)this;
        if (owner.getStatus() != 2 || CookeryFoodProof.recipe(owner, owner.getResult()).isEmpty()) return;
        for (ItemStack portable : callback.getReturnValue()) {
            CompoundTag nativeData = BlockItem.getBlockEntityData(portable);
            if (portable.getItem() instanceof TeapotItem && nativeData != null
                    && ItemStack.matches(TeapotItem.getPourOut(portable), owner.getResult()))
                nativeData.put(CookeryFoodProof.KEY, owner.getPersistentData().getCompound(CookeryFoodProof.KEY).copy());
        }
    }

    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { CookeryFoodProof.load((BlockEntity)(Object)this, tag); }
}
