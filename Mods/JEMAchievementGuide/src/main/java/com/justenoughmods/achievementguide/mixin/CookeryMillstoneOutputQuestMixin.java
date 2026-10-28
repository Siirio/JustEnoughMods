package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity;
import com.justenoughmods.achievementguide.integration.food.MillstoneFoodProof;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity$1", remap = false)
public abstract class CookeryMillstoneOutputQuestMixin {
    @Redirect(method = "onContentsChanged", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/MillstoneBlockEntity;refresh()V"))
    private void jem$mutation(MillstoneBlockEntity owner) {
        MillstoneFoodProof.clear(owner);
        owner.refresh();
    }
}
