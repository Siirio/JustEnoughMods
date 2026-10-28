package com.justenoughmods.achievementguide.integration.caves;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** Admission and post-native observations only, no gameplay or reward authority. */
public final class CaveQuestProof {
    public static final String OWNER = "jemCaveQuestOwnerV4";
    private CaveQuestProof() {}
    public static ServerPlayer actor(Entity actor) { return actor instanceof ServerPlayer player && TravelProof.eligible(player) ? player : null; }
    public static ServerPlayer actor(ServerLevel level, UUID id) { return id == null ? null : actor(level.getServer().getPlayerList().getPlayer(id)); }
    public static ServerPlayer owner(BlockEntity owner) {
        var tag = owner.getPersistentData();
        return owner.getLevel() instanceof ServerLevel level && tag.hasUUID(OWNER) ? actor(level, tag.getUUID(OWNER)) : null;
    }
    public static ServerPlayer owner(Entity owner, String key) {
        return owner.level() instanceof ServerLevel level && owner.getPersistentData().hasUUID(key)
                ? actor(level, owner.getPersistentData().getUUID(key)) : null;
    }
    public static void configured(BlockEntity owner, ServerPlayer actor) {
        if (actor(actor) == null || owner.getLevel() != actor.level()) return;
        owner.getPersistentData().putUUID(OWNER, actor.getUUID()); owner.setChanged();
    }
    public static void emit(Entity actor, String action) { ServerPlayer player = actor(actor); if (player != null) QuestActionTrigger.INSTANCE.emit(player, action); }
    public static boolean item(ItemStack stack, String path) { return !stack.isEmpty() && ("alexscaves:" + path).equals(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()))); }
    public static boolean hostile(Entity entity, Entity actor) {
        return actor != null && entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isAlliedTo(actor)
                && (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER);
    }
    public static boolean damage(Entity target, DamageSource source, float amount, Entity actor, String action) {
        float health = target instanceof LivingEntity living ? living.getHealth() : Float.NaN;
        boolean result = target.hurt(source, amount);
        damageObserved(target, amount, actor, action, health, result);
        return result;
    }
    public static void damageObserved(Entity target, float amount, Entity actor, String action, float health, boolean result) {
        if (result && Float.isFinite(amount) && amount > 0 && Float.isFinite(health) && target instanceof LivingEntity living
                && Float.isFinite(living.getHealth()) && living.getHealth() < health && hostile(target, actor)) emit(actor, action);
    }
    public static boolean loaded(ServerLevel level, BlockPos pos) { return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null; }
    public static boolean permitted(ServerPlayer actor, BlockPos pos) { return actor != null && loaded(actor.serverLevel(), pos) && actor.serverLevel().mayInteract(actor, pos) && actor.mayBuild(); }
    public static int count(net.minecraft.world.Container inventory, ItemStack item) {
        if (inventory.getContainerSize() > 256) return -1;
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, item)) count += stack.getCount();
        }
        return count;
    }
    public static boolean hazmat(ServerPlayer actor) {
        return actor != null && item(actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), "hazmat_mask")
                && item(actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), "hazmat_chestplate")
                && item(actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), "hazmat_leggings")
                && item(actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), "hazmat_boots");
    }
}
