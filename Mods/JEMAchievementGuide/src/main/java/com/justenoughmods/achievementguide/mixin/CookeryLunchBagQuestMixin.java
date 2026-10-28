package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TransmutationLunchBagItem;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ephemeral context stays on this exact native bag for this one native consumption call. */
@Mixin(value = TransmutationLunchBagItem.class, remap = false)
public abstract class CookeryLunchBagQuestMixin {
    @Unique private static final String jem$runKey = "jemNativeLunchBagRunV1";
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack bag, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        if (bag.hasTag()) bag.getTag().remove(jem$runKey);
        if (actor instanceof ServerPlayer) {
            CompoundTag run = new CompoundTag();
            run.putUUID("actor", actor.getUUID());
            bag.getOrCreateTag().put(jem$runKey, run);
        }
    }
    @Redirect(method = "m_5922_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41671_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeMeal(ItemStack meal, Level level, LivingEntity actor, ItemStack bag, Level nativeLevel, LivingEntity nativeActor) {
        int count = meal.getCount();
        ItemStack result = meal.finishUsingItem(level, actor);
        CompoundTag run = jem$run(bag, actor);
        if (run != null && actor == nativeActor && count > 0 && meal.getCount() == count - 1) run.putBoolean("consumed", true);
        return result;
    }
    @Redirect(method = "m_5922_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$sampledEffect(LivingEntity target, MobEffectInstance effect, ItemStack bag, Level level, LivingEntity actor) {
        boolean applied = target.addEffect(effect);
        CompoundTag run = jem$run(bag, actor);
        if (applied && target == actor && run != null && run.getBoolean("consumed")) run.putBoolean("extra_effect", true);
        return applied;
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$finished(ItemStack bag, Level level, LivingEntity actor, CallbackInfoReturnable<ItemStack> callback) {
        CompoundTag run = jem$run(bag, actor);
        boolean success = run != null && run.getBoolean("consumed") && run.getBoolean("extra_effect");
        if (bag.hasTag()) {
            bag.getTag().remove(jem$runKey);
            if (bag.getTag().isEmpty()) bag.setTag(null);
        }
        if (success && actor instanceof ServerPlayer player) QuestActionTrigger.INSTANCE.emit(player, "v3_food_lunch_bag");
    }
    @Unique private static CompoundTag jem$run(ItemStack bag, LivingEntity actor) {
        if (!bag.hasTag() || !bag.getTag().contains(jem$runKey)) return null;
        CompoundTag run = bag.getTag().getCompound(jem$runKey);
        return run.hasUUID("actor") && run.getUUID("actor").equals(actor.getUUID()) ? run : null;
    }
}
