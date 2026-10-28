package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCookingProof;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic$1", remap = false)
public abstract class BackpackCookingInventoryQuestMixin {
    @Redirect(method = "onContentsChanged", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/cooking/CookingLogic;save()V"))
    private void jem$outputChanged(CookingLogic<?> logic, int slot) {
        if (slot == CookingLogic.COOK_OUTPUT_SLOT) BackpackCookingProof.changed(logic);
        ((CookingQuestAccess) logic).jem$saveQuestCookingUpgrade();
    }
}
