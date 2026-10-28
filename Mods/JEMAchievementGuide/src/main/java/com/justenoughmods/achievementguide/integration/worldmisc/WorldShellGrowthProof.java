package com.justenoughmods.achievementguide.integration.worldmisc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Provenance follows the actual native jelly debit, evolution and resulting mount. */
public final class WorldShellGrowthProof {
    private static final String FOOD = "jemShellNativeFoodV4";
    private static final String GROWER = "jemShellNativeGrowerV4";
    public record Pickup(ItemEntity entity, ItemStack variant, int count, ServerPlayer actor) {}
    public record Meal(ItemStack original, int count, CompoundTag proof) {}
    private WorldShellGrowthProof() {}
    public static Pickup pickup(Mob mob, ItemEntity drop) {
        if (!WorldNativeQuestProof.entity(mob, "aquamirae:scuttler") || !WorldNativeQuestProof.nativeMob(mob)
                || !mob.getMainHandItem().isEmpty()) return null;
        return new Pickup(drop, drop.getItem().copy(), drop.getItem().getCount(), WorldNativeQuestProof.supplier(drop));
    }
    public static void picked(Mob mob, Pickup input) {
        if (input == null) return;
        mob.getPersistentData().remove(FOOD);
        ItemStack held = mob.getMainHandItem();
        if (input.actor == null || !WorldNativeQuestProof.real(input.actor) || input.actor.level() != mob.level()
                || !WorldNativeQuestProof.item(input.variant, "aquamirae:jellyfish_jelly") || held.isEmpty()
                || !ItemStack.isSameItemSameTags(input.variant, held)
                || !(input.entity.isRemoved() || input.entity.getItem().getCount() < input.count)) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", input.actor.getUUID());
        proof.put("stack", held.save(new CompoundTag())); mob.getPersistentData().put(FOOD, proof);
    }
    public static Meal eating(Mob mob) {
        return new Meal(mob.getMainHandItem(), mob.getMainHandItem().getCount(), mob.getPersistentData().getCompound(FOOD).copy());
    }
    public static void grew(Mob mob, Meal meal) {
        mob.getPersistentData().remove(GROWER); mob.getPersistentData().remove(FOOD);
        if (meal == null || !(mob.level() instanceof ServerLevel level) || !WorldNativeQuestProof.nativeMob(mob)
                || !meal.proof.hasUUID("actor") || meal.count - meal.original.getCount() != 1) return;
        ItemStack before = ItemStack.of(meal.proof.getCompound("stack"));
        if (!WorldNativeQuestProof.item(before, "aquamirae:jellyfish_jelly") || before.getCount() != meal.count
                || (!meal.original.isEmpty() && !ItemStack.isSameItemSameTags(before, meal.original))) return;
        ServerPlayer actor = WorldNativeQuestProof.actor(level, meal.proof.getUUID("actor"));
        if (actor == null) return;
        mob.getPersistentData().putUUID(GROWER, actor.getUUID());
        if (!meal.original.isEmpty() && mob.getMainHandItem() == meal.original) {
            CompoundTag remaining = meal.proof.copy(); remaining.put("stack", meal.original.save(new CompoundTag()));
            mob.getPersistentData().put(FOOD, remaining);
        }
    }
    public static void evolved(Entity scuttler, Entity child) {
        if (child == null || !scuttler.isRemoved() || child.isRemoved() || child.level() != scuttler.level()
                || !WorldNativeQuestProof.entity(child, "aquamirae:shellback") || !WorldNativeQuestProof.nativeMob(child)
                || !scuttler.getPersistentData().hasUUID(GROWER)) return;
        child.getPersistentData().putUUID(GROWER, scuttler.getPersistentData().getUUID(GROWER));
    }
    public static void mounted(TamableAnimal shell, Entity rider) {
        if (!(rider instanceof ServerPlayer actor) || !WorldNativeQuestProof.real(actor) || rider.getVehicle() != shell
                || !shell.getPassengers().contains(rider) || !shell.isTame() || !actor.getUUID().equals(shell.getOwnerUUID())
                || !shell.getPersistentData().hasUUID(GROWER) || !actor.getUUID().equals(shell.getPersistentData().getUUID(GROWER))) return;
        WorldNativeQuestProof.emit(actor, "v3_pet_shelled_friend");
    }
}
