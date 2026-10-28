package com.justenoughmods.achievementguide.integration.combattools;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

public final class CombatToolProof {
    public static final int LIMIT = 16;
    private CombatToolProof() {}
    public static boolean real(Entity entity) {
        return entity instanceof ServerPlayer actor && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(actor);
    }
    public static String item(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static String type(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static void emit(ServerPlayer actor, String action) { if (real(actor)) QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_" + action); }
    public static int held(ServerPlayer actor, AbstractContainerMenu menu, ItemStack matching) {
        int count = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack actual = actor.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameTags(actual, matching)) count += actual.getCount();
        }
        if (menu != null && ItemStack.isSameItemSameTags(menu.getCarried(), matching)) count += menu.getCarried().getCount();
        return count;
    }
}
