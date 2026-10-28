package com.justenoughmods.achievementguide.integration.magiclast;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
public final class MagicLastProof {
    public static final int LIMIT = 16;
    private MagicLastProof() {}
    public static boolean real(Entity actor) { return actor instanceof ServerPlayer player && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(player); }
    public static String item(ItemStack item) { return BuiltInRegistries.ITEM.getKey(item.getItem()).toString(); }
    public static void emit(ServerPlayer actor, String action) { if (real(actor)) QuestActionTrigger.INSTANCE.emit(actor, action); }
    public static int held(ServerPlayer actor, ItemStack item) { int count = 0; for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) if (ItemStack.isSameItemSameTags(item, actor.getInventory().getItem(slot))) count += actor.getInventory().getItem(slot).getCount(); if (ItemStack.isSameItemSameTags(item, actor.containerMenu.getCarried())) count += actor.containerMenu.getCarried().getCount(); return count; }
}
