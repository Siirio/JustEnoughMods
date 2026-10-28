package com.justenoughmods.achievementguide.mixin;

import com.tom.storagemod.platform.PlatformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.CraftingTerminalBlockEntity", remap = false)
public interface StorageCraftRecipeQuestAccess {
    @Accessor("currentRecipe") PlatformRecipe jem$questRecipe();
}
