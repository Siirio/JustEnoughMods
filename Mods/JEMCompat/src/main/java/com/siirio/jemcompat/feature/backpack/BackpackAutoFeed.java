package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Silent ordinary-food adapter: never invoke hand use, finishUsingItem, sound or particles. */
public final class BackpackAutoFeed {
    private static final String COOLDOWN = "JemAutoFeedNext";
    private static final java.util.Map<Class<?>, Boolean> ORDINARY = new java.util.HashMap<>();
    private static final String FINISH = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(Item.class,
            "m_5922_", ItemStack.class, net.minecraft.world.level.Level.class, net.minecraft.world.entity.LivingEntity.class).getName();
    private static boolean ordinary(Item item) {
        return ORDINARY.computeIfAbsent(item.getClass(), type -> {
            try {
                return type.getMethod(FINISH, ItemStack.class, net.minecraft.world.level.Level.class,
                        net.minecraft.world.entity.LivingEntity.class).getDeclaringClass() == Item.class;
            } catch (ReflectiveOperationException failure) { return false; }
        });
    }
    private BackpackAutoFeed() { }
    public static void tick(ServerPlayer player, BackpackInventory inventory) {
        long now = player.level().getGameTime();
        if (!player.canEat(false) || player.getPersistentData().getLong(COOLDOWN) > now) return;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack food = inventory.getItem(slot);
            var properties = food.getFoodProperties(player);
            // A custom item may implement finishUsingItem effects/containers invisible to FoodProperties.
            // Refuse those and NBT-bearing/effect/always-edible foods until an explicit exact adapter exists.
            if (food.isEmpty() || !inventory.canPlaceItem(slot, food) || !ordinary(food.getItem()) || food.hasTag() || food.getRarity() != net.minecraft.world.item.Rarity.COMMON || properties == null
                    || properties.getNutrition() <= 0 || properties.canAlwaysEat() || !properties.getEffects().isEmpty()) continue;
            ItemStack remainder = food.getCraftingRemainingItem();
            if (!remainder.isEmpty() && !BackpackInventory.isAllowedItem(remainder)) continue;
            // Reserve the only necessary return destination before mutating hunger or the food stack.
            int returnSlot = -1;
            if (!remainder.isEmpty() && food.getCount() > 1) {
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack present = inventory.getItem(i);
                    if (inventory.canPlaceItem(i, remainder) && (present.isEmpty()
                            || ItemStack.isSameItemSameTags(present, remainder) && present.getCount() + remainder.getCount() <= present.getMaxStackSize())) {
                        returnSlot = i; break;
                    }
                }
                if (returnSlot < 0) continue;
            }
            player.getFoodData().eat(food.getItem(), food, player);
            food.shrink(1);
            if (!remainder.isEmpty()) {
                if (food.isEmpty()) inventory.setItem(slot, remainder);
                else if (inventory.getItem(returnSlot).isEmpty()) inventory.setItem(returnSlot, remainder);
                else inventory.getItem(returnSlot).grow(remainder.getCount());
            }
            inventory.setChanged();
            inventory.saveItemsToStack();
            player.getPersistentData().putLong(COOLDOWN, now + 40);
            return;
        }
    }
}
