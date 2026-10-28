package com.justenoughmods.achievementguide.mixin;

import com.bmt.kaleidoscope_nether.item.EverlastingFoodItem;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native enabled/food admission eats a copy, applies its own cooldown and retains the original stack. */
@Mixin(value = EverlastingFoodItem.class, remap = false)
public abstract class EverlastingFoodQuestMixin {
    @Unique private static final String jem$runKey = "jemNativeRetainedSteakRunV1";
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack retained, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        if (actor instanceof ServerPlayer player) {
            player.getPersistentData().remove(jem$runKey);
            if (FoodOutputProof.item(retained, "kaleidoscope_nether:everlasting_flame_steak")) {
                CompoundTag run = new CompoundTag();
                run.put("original", retained.save(new CompoundTag()));
                player.getPersistentData().put(jem$runKey, run);
            }
        }
    }
    @Redirect(method = "m_5922_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_5584_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeMeal(LivingEntity actor, Level level, ItemStack consumed, ItemStack retained,
                                     Level nativeLevel, LivingEntity nativeActor) {
        int count = consumed.getCount();
        ItemStack result = actor.eat(level, consumed);
        if (actor instanceof ServerPlayer player && actor == nativeActor && level == nativeLevel && count > 0
                && consumed.getCount() == count - 1 && player.getPersistentData().contains(jem$runKey))
            player.getPersistentData().getCompound(jem$runKey).putBoolean("consumed", true);
        return result;
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$finished(ItemStack retained, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        if (!(actor instanceof ServerPlayer player)) return;
        CompoundTag run = player.getPersistentData().getCompound(jem$runKey).copy();
        player.getPersistentData().remove(jem$runKey);
        if (run.getBoolean("consumed") && callback.getReturnValue() == retained
                && ItemStack.matches(retained, ItemStack.of(run.getCompound("original"))))
            QuestActionTrigger.INSTANCE.emit(player, "v3_food_nether_provisions");
    }
}
