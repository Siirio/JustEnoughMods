package com.siirio.jemcompat.compat.equipment;

import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.MendingEnchantment;

public final class InfiniteEquipmentDurability {
    private static final int WEAPON = 2031;
    private static final int HELMET = 407;
    private static final int CHESTPLATE = 592;
    private static final int LEGGINGS = 555;
    private static final int BOOTS = 481;

    private InfiniteEquipmentDurability() {
    }

    public static boolean applies(ItemStack stack) {
        return isEquipment(stack) && !stack.getItem().isDamageable(stack);
    }

    public static int maxDamage(ItemStack stack) {
        if (!applies(stack)) {
            return 0;
        }
        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return stack.getItem() instanceof ElytraItem ? CHESTPLATE : WEAPON;
        }
        EquipmentSlot slot = armor.getEquipmentSlot();
        return switch (slot) {
            case HEAD -> HELMET;
            case CHEST -> CHESTPLATE;
            case LEGS -> LEGGINGS;
            case FEET -> BOOTS;
            default -> WEAPON;
        };
    }

    public static boolean acceptsDurabilityEnchantment(ItemStack stack, Enchantment enchantment) {
        return applies(stack)
                && (enchantment instanceof DigDurabilityEnchantment || enchantment instanceof MendingEnchantment);
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof ElytraItem
                || ProgressionEnchantment.isWeaponEligible(stack);
    }
}
