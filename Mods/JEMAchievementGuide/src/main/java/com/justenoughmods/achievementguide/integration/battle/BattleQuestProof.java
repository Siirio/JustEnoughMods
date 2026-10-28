package com.justenoughmods.achievementguide.integration.battle;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** Only translates committed native outcomes, never supplies combat eligibility or mechanics. */
public final class BattleQuestProof {
    public static final int LIMIT = 16;
    private BattleQuestProof() {}
    public static boolean real(Entity entity) {
        return entity instanceof ServerPlayer player && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(player);
    }
    public static String item(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static String type(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static boolean natural(Entity entity) {
        return !entity.getPersistentData().getBoolean("jem:event_spawned")
                && !entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance").equals("RAID_EVENT");
    }
    public static boolean hostile(LivingEntity entity) {
        return entity instanceof Enemy && !(entity instanceof Player)
                && !(entity instanceof TamableAnimal tame && tame.isTame()) && natural(entity);
    }
    public static void emit(Entity entity, String action) {
        if (real(entity)) QuestActionTrigger.INSTANCE.emit((ServerPlayer)entity, action);
    }
    public static int held(ServerPlayer player, ItemStack sample) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(sample, stack)) count += stack.getCount();
        }
        return count;
    }
}
