package com.justenoughmods.achievementguide.mixin;

import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer", remap = false)
public interface BackpackCraftRecipeQuestAccess {
    @Accessor("lastRecipe") CraftingRecipe jem$questRecipe();
}
