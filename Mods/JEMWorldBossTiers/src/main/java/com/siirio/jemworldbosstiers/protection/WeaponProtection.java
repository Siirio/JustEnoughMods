package com.siirio.jemworldbosstiers.protection;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import com.siirio.jemworldbosstiers.network.ClientTierState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WeaponProtection {
    private static final int EXTRA_LIFETIME = 6000;

    @SubscribeEvent
    public void protectDroppedWeapon(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof ItemEntity item && protectedEquipment(item.getItem())) {
            protect(item);
        }
    }

    @SubscribeEvent
    public void preventExpiry(ItemExpireEvent event) {
        if (protectedEquipment(event.getEntity().getItem())) {
            event.setExtraLife(EXTRA_LIFETIME);
        }
    }

    @SubscribeEvent
    public void rescueFromVoid(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ItemEntity item)
                || !protectedEquipment(item.getItem())
                || item.getY() >= level.getMinBuildHeight() - 16
                || item.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || item.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        ItemStack rescuedStack = item.getItem().copy();
        level.getServer().execute(() -> {
            ItemEntity rescued = new ItemEntity(level, 0.0D, 0.0D, 0.0D, rescuedStack);
            rescued.setPos(Vec3.atCenterOf(level.getSharedSpawnPos()).add(0.0D, 2.0D, 0.0D));
            protect(rescued);
            level.addFreshEntity(rescued);
        });
    }

    public static boolean protectedEquipment(ItemStack stack) {
        return !stack.isEmpty() && (progressionEquipment(stack) || bossEquipment(stack));
    }

    public static boolean bossWeapon(ItemStack stack) {
        net.minecraft.resources.ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return BalanceRegistry.reward(itemId)
                .map(profile -> profile.category().equals("weapon") || profile.category().endsWith("_weapon"))
                .orElseGet(() -> ClientTierState.isBossWeapon(itemId));
    }

    private static boolean progressionEquipment(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), stack) > 0;
    }

    private static boolean bossEquipment(ItemStack stack) {
        net.minecraft.resources.ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return BalanceRegistry.reward(itemId)
                .map(profile -> profile.category().equals("weapon")
                        || profile.category().equals("armor")
                        || profile.category().endsWith("_weapon")
                        || profile.category().endsWith("_armor")
                        || profile.category().endsWith("_shield"))
                .orElse(false);
    }

    private static void protect(ItemEntity item) {
        item.setInvulnerable(true);
        item.setUnlimitedLifetime();
        item.clearFire();
    }
}
