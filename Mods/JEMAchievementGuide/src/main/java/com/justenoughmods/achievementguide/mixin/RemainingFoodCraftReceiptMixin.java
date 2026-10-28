package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class RemainingFoodCraftReceiptMixin {
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private Player player;
    @Unique private Recipe<?> jem$recipe;
    @Unique private Recipe<?> jem$quickRecipe;
    @Unique private long jem$quickTick;
    @Unique private final List<ItemStack> jem$removed = new ArrayList<>();
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$begin(Player player, ItemStack result, CallbackInfo callback) {
        jem$removed.clear(); jem$recipe = null;
        if (player instanceof ServerPlayer && ((Slot)(Object)this).container instanceof RecipeHolder owner) {
            jem$recipe = owner.getRecipeUsed();
            if (jem$recipe == null && jem$quickTick == player.level().getGameTime() && jem$quickRecipe instanceof CraftingRecipe crafting
                    && crafting.matches(craftSlots, player.level())) jem$recipe = jem$quickRecipe;
        }
        jem$quickRecipe = null;
    }
    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void jem$nativeQuickRecipe(ItemStack result, CallbackInfo callback) {
        if (player instanceof ServerPlayer && ((Slot)(Object)this).container instanceof RecipeHolder owner && owner.getRecipeUsed() != null) {
            jem$quickRecipe = owner.getRecipeUsed(); jem$quickTick = player.level().getGameTime();
        }
    }
    @Redirect(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$actualDebit(CraftingContainer inventory, int slot, int amount) {
        ItemStack removed = inventory.removeItem(slot, amount);
        if ((Object)this instanceof com.justenoughmods.achievementguide.integration.CaveResearchProof.CraftDebitWitness witness)
            witness.jem$caveMapDebit(inventory, amount, removed);
        if (inventory == craftSlots && !removed.isEmpty() && jem$removed.size() < 9) jem$removed.add(removed.copy());
        return removed;
    }
    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$finished(Player player, ItemStack result, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor) NativePreparedMealProof.crafted(actor, jem$recipe, NativePreparedMealProof.actualCraftOutput(actor,result), jem$removed);
        jem$recipe = null; jem$quickRecipe = null; jem$removed.clear();
    }
}
