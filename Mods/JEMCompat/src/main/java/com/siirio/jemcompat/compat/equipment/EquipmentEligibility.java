package com.siirio.jemcompat.compat.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.fml.ModList;

public final class EquipmentEligibility {
    private EquipmentEligibility() { }

    public static boolean weapon(ItemStack stack) {
        if (ModList.get().isLoaded("jem_world_boss_tiers")) return WorldTierEquipment.weapon(stack);
        var item = stack.getItem();
        UseAnim animation = stack.getUseAnimation();
        return item instanceof net.minecraft.world.item.SwordItem || item instanceof net.minecraft.world.item.DiggerItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem || item instanceof net.minecraft.world.item.TridentItem
                || item instanceof net.minecraft.world.item.ShieldItem
                || animation == UseAnim.BOW || animation == UseAnim.CROSSBOW || animation == UseAnim.SPEAR
                || stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
                        .anyMatch(modifier -> modifier.getAmount() > 0);
    }

    private static final class WorldTierEquipment {
        private static boolean weapon(ItemStack stack) {
            return com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment.isWeaponEligible(stack);
        }
    }
}
