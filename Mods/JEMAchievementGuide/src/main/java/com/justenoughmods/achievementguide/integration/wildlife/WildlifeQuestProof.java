package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** Observational native entity lineage, with completion owned by advancement criteria. */
public final class WildlifeQuestProof {
    public static final String KEY = "JemWildlifeQuestV4";
    private WildlifeQuestProof() {}
    public static ServerPlayer real(Player player) {
        return player instanceof ServerPlayer actor && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(actor)
                && actor.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL ? actor : null;
    }
    public static String item(ItemStack stack) { return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static String type(Entity entity) { return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())); }
    public static CompoundTag data(Entity entity) {
        var root = entity.getPersistentData(); if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag()); return root.getCompound(KEY);
    }
    public static void mark(Entity entity, String nativeStage, UUID actor) {
        if (entity.level() instanceof ServerLevel && actor != null) data(entity).putUUID(nativeStage, actor);
    }
    public static boolean matches(Entity entity, String nativeStage, UUID actor) {
        var tag = data(entity); return actor != null && tag.hasUUID(nativeStage) && actor.equals(tag.getUUID(nativeStage));
    }
    public static ServerPlayer actor(Entity entity, String nativeStage) {
        if (!(entity.level() instanceof ServerLevel level) || !data(entity).hasUUID(nativeStage)) return null;
        return real(level.getServer().getPlayerList().getPlayer(data(entity).getUUID(nativeStage)));
    }
    public static ServerPlayer owner(TamableAnimal entity) {
        return entity.isTame() && entity.getOwner() instanceof Player player ? real(player) : null;
    }
    public static boolean hostile(LivingEntity entity, ServerPlayer actor) {
        return actor != null && entity != actor && !entity.isAlliedTo(actor)
                && (entity instanceof Enemy || entity.getLastHurtMob() == actor || actor.getLastHurtByMob() == entity);
    }
    public static boolean debited(ItemStack before, ItemStack after, int amount) {
        return amount > 0 && before.getCount() - after.getCount() == amount && (after.isEmpty() || ItemStack.isSameItemSameTags(before, after));
    }
    public static void emit(ServerPlayer actor, String action) { if (real(actor) != null) QuestActionTrigger.INSTANCE.emit(actor, action); }
}
