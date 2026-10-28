package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatCraftProof;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResultSlot.class)
public abstract class CombatCraftTakeQuestMixin {
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private Player player;
    @Inject(method = "remove", at = @At("HEAD"))
    private void jem$prepare(int amount, CallbackInfoReturnable<ItemStack> callback) { CombatCraftProof.prepare(player, ((Slot)(Object)this).getItem()); }
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$begin(Player actor, ItemStack result, CallbackInfo callback) {
        CombatCraftProof.begin(actor, craftSlots, ((Slot)(Object)this).container instanceof RecipeHolder holder ? holder.getRecipeUsed() : null, result);
    }
    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$end(Player actor, ItemStack result, CallbackInfo callback) { CombatCraftProof.end(); }
}
