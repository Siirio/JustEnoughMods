package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only the native blank-book recording branch can reach this transfer. No item singleton state. */
@Mixin(value = RecipeItem.class, remap = false)
public abstract class CookeryRecipeRecordQuestMixin {
    @Redirect(method = "onRecordRecipe", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/util/ItemUtils;getItemToLivingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$recorded(LivingEntity recipient, ItemStack book, Level level, Player player,
                             BlockEntity station, RecipeManager recipes, ItemStack blank) {
        RecipeItem.RecipeRecord record = RecipeItem.getRecipe(book);
        boolean valid = recipient == player && player instanceof ServerPlayer
                && !RecipeItem.hasRecipe(blank) && record != null && !record.output().isEmpty()
                && !record.input().isEmpty() && record.input().size() <= 16;
        if (valid && station instanceof PotBlockEntity pot) {
            valid = pot.getStatus() == 0 && record.type().equals(RecipeItem.POT)
                    && jem$sameInputs(record.input(), pot.getInputs())
                    && (record.flexRecipe() ? recipes.getRecipeFor(ModRecipes.FLEX_POT_RECIPE, pot.getContainer(), level).isPresent()
                                           : recipes.getRecipeFor(ModRecipes.POT_RECIPE, pot.getContainer(), level).isPresent());
        } else if (valid && station instanceof StockpotBlockEntity pot) {
            valid = pot.getStatus() == 1 && record.type().equals(RecipeItem.STOCKPOT)
                    && jem$sameInputs(record.input(), pot.getInputs())
                    && (record.flexRecipe() ? recipes.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, pot.getContainer(), level).isPresent()
                                           : recipes.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, pot.getContainer(), level).isPresent());
        } else valid = false;
        ItemStack written = valid ? book.copy() : ItemStack.EMPTY;
        int held = valid ? FoodOutputProof.held((ServerPlayer)player, player.containerMenu, written) : 0;
        ItemUtils.getItemToLivingEntity(recipient, book);
        if (valid && FoodOutputProof.held((ServerPlayer)player, player.containerMenu, written) > held)
            QuestActionTrigger.INSTANCE.emit((ServerPlayer)player, "food/recipe_recorded");
    }
    @Unique private static boolean jem$sameInputs(List<ItemStack> recorded, List<ItemStack> nativeInputs) {
        if (nativeInputs.size() > 16) return false;
        List<ItemStack> actual = nativeInputs.stream().filter(item -> !item.isEmpty()).toList();
        if (recorded.size() != actual.size()) return false;
        for (int slot = 0; slot < actual.size(); slot++) if (!ItemStack.matches(recorded.get(slot), actual.get(slot))) return false;
        return true;
    }
}
