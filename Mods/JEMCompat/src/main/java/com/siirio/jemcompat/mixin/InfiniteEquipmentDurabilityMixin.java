package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.compat.equipment.InfiniteEquipmentDurability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class InfiniteEquipmentDurabilityMixin {
    @Inject(method = "isDamageableItem", at = @At("HEAD"), cancellable = true)
    private void jemcompat$enableEquipmentDurability(CallbackInfoReturnable<Boolean> callback) {
        ItemStack stack = (ItemStack) (Object) this;
        if (InfiniteEquipmentDurability.applies(stack)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void jemcompat$useNetheriteDurability(CallbackInfoReturnable<Integer> callback) {
        int durability = InfiniteEquipmentDurability.maxDamage((ItemStack) (Object) this);
        if (durability > 0) {
            callback.setReturnValue(durability);
        }
    }
}
