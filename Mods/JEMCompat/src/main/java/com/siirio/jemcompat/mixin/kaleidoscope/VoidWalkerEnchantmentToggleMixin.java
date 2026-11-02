package com.siirio.jemcompat.mixin.kaleidoscope;

import com.siirio.jemcompat.feature.kaleidoscope.VoidWalkerEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers native collision and jump handlers even though they are merged into separate methods. */
@Mixin(EnchantmentHelper.class)
public abstract class VoidWalkerEnchantmentToggleMixin {
    @Inject(method="getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
            at=@At("HEAD"), cancellable=true, require=1)
    private static void jem$disabledVoidWalker(Enchantment enchantment, LivingEntity entity,
                                              CallbackInfoReturnable<Integer> callback) {
        if (VoidWalkerEvents.suppressed(enchantment, entity)) callback.setReturnValue(0);
    }
}
