package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCookingProof;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic", remap = false)
public abstract class BackpackCookingQuestMixin {
    @Unique private BackpackCookingProof.Before jem$cookingBefore;
    @Inject(method = "smelt", at = @At("HEAD"))
    private void jem$beforeSmelt(Recipe<?> recipe, Level level, CallbackInfo callback) {
        jem$cookingBefore = BackpackCookingProof.before((CookingLogic<?>) (Object) this, recipe, level);
    }
    @Inject(method = "smelt", at = @At("RETURN"))
    private void jem$afterSmelt(Recipe<?> recipe, Level level, CallbackInfo callback) {
        var before = jem$cookingBefore;
        jem$cookingBefore = null;
        BackpackCookingProof.completed((CookingLogic<?>) (Object) this, before);
    }
}
