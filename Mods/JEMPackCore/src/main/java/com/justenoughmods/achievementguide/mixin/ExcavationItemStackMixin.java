package com.justenoughmods.achievementguide.mixin;

import com.siirio.jempackcore.enchantment.ExcavationMining;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStack.class)
public abstract class ExcavationItemStackMixin {
    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I"))
    private int excavationUnbreaking(Enchantment enchantment, ItemStack stack) {
        return ExcavationMining.bypassUnbreaking(stack) ? 0
                : EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }
}
