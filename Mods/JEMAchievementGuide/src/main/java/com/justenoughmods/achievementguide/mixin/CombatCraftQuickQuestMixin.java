package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatCraftProof;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CraftingMenu.class, InventoryMenu.class})
public abstract class CombatCraftQuickQuestMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void jem$begin(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) { CombatCraftProof.quickBegin(actor, (AbstractContainerMenu)(Object)this, slot); }
    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void jem$end(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) { CombatCraftProof.quickEnd(); }
}
