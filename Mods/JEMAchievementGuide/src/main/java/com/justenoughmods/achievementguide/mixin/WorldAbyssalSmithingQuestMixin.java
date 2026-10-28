package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldAbyssalSmithingProof;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class WorldAbyssalSmithingQuestMixin {
    @Shadow private SmithingRecipe selectedRecipe;
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jem$begin(Player actor, ItemStack result, CallbackInfo callback) {
        WorldAbyssalSmithingProof.begin((SmithingMenu)(Object)this, selectedRecipe, actor, result);
    }
    @Inject(method = "onTake", at = @At("RETURN"))
    private void jem$end(Player actor, ItemStack result, CallbackInfo callback) {
        WorldAbyssalSmithingProof.end((SmithingMenu)(Object)this, actor);
    }
}
