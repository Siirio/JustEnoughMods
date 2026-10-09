package com.siirio.jemcompat.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemDurabilityBarMixin {
    @Redirect(
            method = {"getBarWidth", "getBarColor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item;getMaxDamage(Lnet/minecraft/world/item/ItemStack;)I",
                    remap = false
            )
    )
    private int jemcompat$useStackDurability(Item item, ItemStack stack) {
        return stack.getMaxDamage();
    }
}
