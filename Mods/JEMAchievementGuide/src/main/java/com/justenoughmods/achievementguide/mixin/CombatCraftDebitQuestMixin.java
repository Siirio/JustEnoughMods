package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatCraftProof;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransientCraftingContainer.class)
public abstract class CombatCraftDebitQuestMixin {
    @Inject(method = "removeItem", at = @At("RETURN"))
    private void jem$debit(int slot, int amount, CallbackInfoReturnable<ItemStack> callback) { CombatCraftProof.removed((CraftingContainer)(Object)this, slot, callback.getReturnValue()); }
}
