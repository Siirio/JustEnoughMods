package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import com.justenoughmods.achievementguide.integration.CaveResearchProof;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class CaveMapResultQuestMixin implements CaveResearchProof.CraftDebitWitness {
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private Player player;
    @Unique private String jem$biome = "";
    @Unique private int jem$debits;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$beforeMapCraft(Player operator, ItemStack result, CallbackInfo callback) {
        jem$biome = "";
        jem$debits = 0;
        if (operator != player || !(operator instanceof ServerPlayer actor)
                || !(actor.containerMenu instanceof CraftingMenu menu)
                || menu.getSlot(0) != (Object) this || !menu.stillValid(actor) || craftSlots.getContainerSize() != 9) return;
        ItemStack taken = result.isEmpty() ? ((QuestResultSnapshot) menu).jem$questQuickResult() : result;
        String biome = CaveResearchProof.biome(taken, "alexscaves:cave_map");
        if (biome.isEmpty() || !biome.equals(CaveResearchProof.biome(craftSlots.getItem(4), "alexscaves:cave_codex"))) return;
        var recipe = actor.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, actor.level()).orElse(null);
        if (recipe != null && recipe.getId().equals(new ResourceLocation("alexscaves", "cave_map"))
                && recipe.matches(craftSlots, actor.level())) jem$biome = biome;
    }

    @Override
    public void jem$caveMapDebit(CraftingContainer matrix, int count, ItemStack removed) {
        if (!jem$biome.isEmpty() && matrix == craftSlots && count == 1 && removed.getCount() == 1) jem$debits++;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void jem$mapCrafted(Player operator, ItemStack result, CallbackInfo callback) {
        String biome = jem$biome;
        int debits = jem$debits;
        jem$biome = "";
        jem$debits = 0;
        if (operator == player && operator instanceof ServerPlayer actor && debits == 9) CaveResearchProof.craftedMap(actor, biome);
    }
}
