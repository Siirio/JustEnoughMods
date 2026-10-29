package com.siirio.jemcompat.mixin.kaleidoscope;

import com.siirio.jemcompat.feature.kaleidoscope.VoidWalkerEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=LivingEntity.class,priority=500)
public abstract class VoidWalkerJumpToggleMixin {
    @Redirect(method="jumpFromGround",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I"),require=0)
    private int jem$toggleVoidWalkerJump(Enchantment enchantment,LivingEntity entity) {
        return VoidWalkerEvents.effectiveLevel(enchantment,entity);
    }
}
