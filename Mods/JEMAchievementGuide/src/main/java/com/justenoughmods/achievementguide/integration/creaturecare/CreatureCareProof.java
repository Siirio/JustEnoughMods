package com.justenoughmods.achievementguide.integration.creaturecare;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** Same-entity native-operation witnesses, never a second quest progress store. */
public final class CreatureCareProof {
    public static final String KEY = "JemCreatureCareV4";
    private CreatureCareProof() {}
    public static ServerPlayer real(Player player) {
        return player instanceof ServerPlayer actor && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(actor) ? actor : null;
    }
    public static String item(ItemStack stack) { return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static String type(Entity entity) { return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())); }
    public static CompoundTag data(Entity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }
    public static void mark(Entity entity, String stage, UUID actor) {
        if (entity.level() instanceof ServerLevel && actor != null) data(entity).putUUID(stage, actor);
    }
    public static boolean matches(Entity entity, String stage, UUID actor) {
        CompoundTag tag = data(entity);
        return actor != null && tag.hasUUID(stage) && actor.equals(tag.getUUID(stage));
    }
    public static ServerPlayer actor(Entity entity, String stage) {
        CompoundTag tag = data(entity);
        if (!(entity.level() instanceof ServerLevel level) || !tag.hasUUID(stage)) return null;
        return real(level.getServer().getPlayerList().getPlayer(tag.getUUID(stage)));
    }
    public static boolean owned(TamableAnimal animal, ServerPlayer actor) {
        return actor != null && animal.isTame() && actor.getUUID().equals(animal.getOwnerUUID());
    }
    public static void emit(ServerPlayer actor, String action) { if (real(actor) != null) QuestActionTrigger.INSTANCE.emit(actor, action); }
    public static boolean threat(ServerPlayer actor, LivingEntity target) {
        return actor != null && (actor.getLastHurtByMob() == target || target.getLastHurtMob() == actor);
    }
    public static void defended(TamableAnimal animal, LivingEntity target, float healthBefore, boolean accepted, String stage) {
        ServerPlayer actor = actor(animal, stage);
        if (accepted && target.getHealth() < healthBefore && owned(animal, actor) && threat(actor, target)) {
            emit(actor, "v3_fauna_woodland_care");
        }
    }
    public static boolean debited(ItemStack before, ItemStack after) {
        return before.getCount() - after.getCount() == 1 && (after.isEmpty() || ItemStack.isSameItemSameTags(before, after));
    }
}
