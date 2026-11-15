package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-owned equipment admission for every native Recruits inventory path. */
public final class RecruitEquipmentPolicy {
    private static final Set<net.minecraft.world.item.Item> ALLOWED = Set.of(
            Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            Items.TURTLE_HELMET,
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.GOLDEN_AXE, Items.IRON_AXE, Items.DIAMOND_AXE,
            Items.BOW, Items.CROSSBOW, Items.SHIELD
    );

    private RecruitEquipmentPolicy() { }

    public static boolean allowed(ItemStack stack) {
        return stack.isEmpty() || ALLOWED.contains(stack.getItem());
    }

    static void enforce(AbstractRecruitEntity recruit) {
        if (recruit.level().isClientSide()) return;
        var inventory = recruit.getInventory();
        for (int slot = 0; slot <= 5 && slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (allowed(stack)) continue;
            ItemStack returned = stack.copy();
            inventory.setItem(slot, ItemStack.EMPTY);
            EquipmentSlot equipmentSlot = recruit.getEquipmentSlotIndex(slot);
            if (equipmentSlot != null) recruit.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            returnSafely(recruit, returned);
            String id = BuiltInRegistries.ITEM.getKey(returned.getItem()).toString();
            if (recruit.getOwner() instanceof ServerPlayer owner) {
                owner.sendSystemMessage(Component.translatable("message.jem_settlements.recruit_equipment_rejected", id));
            }
        }
        recruit.onInventoryChanged();
    }

    private static void returnSafely(AbstractRecruitEntity recruit, ItemStack stack) {
        var inventory = recruit.getInventory();
        for (int slot = 6; slot < inventory.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack present = inventory.getItem(slot);
            if (present.isEmpty()) {
                inventory.setItem(slot, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameTags(present, stack) && present.getCount() < present.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), present.getMaxStackSize() - present.getCount());
                present.grow(moved);
                stack.shrink(moved);
            }
        }
        if (!stack.isEmpty() && recruit.getOwner() instanceof ServerPlayer owner && owner.getInventory().add(stack)) return;
        if (!stack.isEmpty()) recruit.spawnAtLocation(stack);
    }
}
