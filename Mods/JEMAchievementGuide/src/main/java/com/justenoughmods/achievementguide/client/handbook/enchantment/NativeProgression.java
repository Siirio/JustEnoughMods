package com.justenoughmods.achievementguide.client.handbook.enchantment;

import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Reads the existing World Tier snapshot and eligibility; never keeps a second tier state. */
final class NativeProgression {
    private static Method tier;
    private static Method eligible;
    private static Method armor;

    private NativeProgression() { }

    static Component currentEffect(ItemStack stack) {
        try {
            if (tier == null) {
                tier = Class.forName("com.siirio.jemworldbosstiers.network.ClientTierState").getMethod("tier");
                Class<?> owner = Class.forName("com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment");
                eligible = owner.getMethod("isEligible", ItemStack.class);
                armor = owner.getMethod("isArmorEligible", ItemStack.class);
            }
            if (!(Boolean) eligible.invoke(null, stack)) return null;
            String kind = (Boolean) armor.invoke(null, stack) ? "armor" : "weapon";
            int currentTier = (Integer) tier.invoke(null);
            return Component.translatable("tooltip.jem_world_boss_tiers.progression." + kind + "." + Math.min(3, currentTier));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("The registered Progression enchantment owner API is unavailable", exception);
        }
    }
}
