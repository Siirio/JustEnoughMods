package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.VoidMeal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.li64.tide.registries.items.VoidseekerItem", remap = false)
public abstract class RemainingVoidseekerQuestMixin {
    @Unique private static final ThreadLocal<VoidMeal> jem$meal = new ThreadLocal<>();
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$begin(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) {
        jem$meal.remove();
        if (entity instanceof ServerPlayer actor && RemainingFoodActors.real(actor) != null) jem$meal.set(new VoidMeal(actor, stack, stack.getCount()));
    }
    @Redirect(method = "lambda$finishUsingItem$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;m_8999_(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"))
    private static void jem$nativeReturn(ServerPlayer actor, ServerLevel destination, double x, double y, double z, float yaw, float pitch) {
        Vec3 before = actor.position();
        Level origin = actor.level();
        actor.teleportTo(destination, x, y, z, yaw, pitch);
        VoidMeal meal = jem$meal.get();
        if (meal != null && meal.actor() == actor && meal.count() - meal.stack().getCount() == 1
                && actor.level() == destination && actor.position().equals(new Vec3(x, y, z))
                && (origin != destination || !before.equals(actor.position()))) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_fish_home");
        }
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$end(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) { jem$meal.remove(); }
}
