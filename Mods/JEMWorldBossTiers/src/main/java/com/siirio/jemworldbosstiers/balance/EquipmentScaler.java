package com.siirio.jemworldbosstiers.balance;

import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;
import com.siirio.jemworldbosstiers.network.ClientTierState;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class EquipmentScaler {
    private EquipmentScaler() {
    }

    public static void apply(ItemAttributeModifierEvent event) {
        addProgressionContributions(event);
    }

    private static void addProgressionContributions(ItemAttributeModifierEvent event) {
        if (EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), event.getItemStack()) <= 0
                || !ProgressionEnchantment.isEligible(event.getItemStack())
                || currentTier() < 2) {
            return;
        }
        int tier = currentTier();
        if (ProgressionEnchantment.isArmorEligible(event.getItemStack())) {
            addArmorProgressionContributions(event, tier);
            return;
        }
        if (event.getSlotType() != EquipmentSlot.MAINHAND
                || event.getItemStack().getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            return;
        }
        double damage = tier >= 3 ? 2.0D : 1.0D;
        double speed = tier >= 3 ? 0.40D : 0.25D;
        addProgressionContribution(event, Attributes.ATTACK_DAMAGE, damage);
        addProgressionContribution(event, Attributes.ATTACK_SPEED, speed);
    }

    private static void addArmorProgressionContributions(ItemAttributeModifierEvent event, int tier) {
        ArmorItem armor = (ArmorItem) event.getItemStack().getItem();
        if (armor.getType().getSlot() != event.getSlotType()) {
            return;
        }
        double multiplier = tier >= 3 ? 0.10D : 0.05D;
        addProgressionContribution(event, Attributes.ARMOR, armor.getDefense() * multiplier);
        addProgressionContribution(event, Attributes.ARMOR_TOUGHNESS, armor.getToughness() * multiplier);
        if (event.getSlotType() == EquipmentSlot.FEET) {
            addProgressionContribution(event, Attributes.KNOCKBACK_RESISTANCE, tier >= 3 ? 0.25D : 0.15D);
        }
    }

    private static void addProgressionContribution(ItemAttributeModifierEvent event, Attribute attribute, double amount) {
        ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        event.addModifier(attribute, new AttributeModifier(
                progressionModifierId(event.getSlotType(), attributeId),
                "JEM progression tier",
                amount,
                AttributeModifier.Operation.ADDITION
        ));
    }

    public static void refresh(net.minecraft.server.level.ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), stack) <= 0) {
                continue;
            }
            var modifiers = stack.getAttributeModifiers(slot);
            player.getAttributes().removeAttributeModifiers(modifiers);
            player.getAttributes().addTransientAttributeModifiers(modifiers);
        }
    }

    private static int currentTier() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return WorldTierData.get(ServerLifecycleHooks.getCurrentServer()).tier();
        }
        return ClientTierState.tier();
    }

    private static UUID progressionModifierId(EquipmentSlot slot, ResourceLocation attributeId) {
        String key = "jem-world-boss-tiers:progression:" + slot.getName() + ':' + attributeId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
