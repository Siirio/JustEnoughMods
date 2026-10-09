package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.compat.equipment.InfiniteEquipmentDurability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class DurabilityEnchantmentMixin {
    @Inject(method = "canApplyAtEnchantingTable", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$allowFallbackDurabilityEnchantments(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        Enchantment enchantment = (Enchantment) (Object) this;
        if (InfiniteEquipmentDurability.acceptsDurabilityEnchantment(stack, enchantment)) {
            callback.setReturnValue(true);
        }
    }
}
