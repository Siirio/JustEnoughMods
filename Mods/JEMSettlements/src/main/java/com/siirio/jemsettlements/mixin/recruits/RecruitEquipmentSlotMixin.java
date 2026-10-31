package com.siirio.jemsettlements.mixin.recruits;

import com.siirio.jemsettlements.compat.recruits.RecruitEquipmentPolicy;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects forbidden equipment before native GUI slots can debit the player's stack. */
@Pseudo
@Mixin(targets = {
        "com.talhanation.recruits.inventory.RecruitInventoryMenu$1",
        "com.talhanation.recruits.inventory.RecruitInventoryMenu$2",
        "com.talhanation.recruits.inventory.RecruitInventoryMenu$3"
})
public abstract class RecruitEquipmentSlotMixin {
    @Inject(method = "m_5857_", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$restrictEquipment(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!RecruitEquipmentPolicy.allowed(stack)) callback.setReturnValue(false);
    }
}
