package com.justenoughmods.achievementguide.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Records only an actual accepted native crossbow projectile, without awarding a quest on launch. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowArrowQuestMixin {
    @Redirect(method = "shootProjectile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$acceptedCrossbowArrow(Level level, Entity result, Level nativeLevel, LivingEntity shooter,
                                                     InteractionHand hand, ItemStack crossbow, ItemStack ammunition,
                                                     float soundPitch, boolean creative, float speed, float spread, float angle) {
        boolean accepted = level.addFreshEntity(result);
        if (accepted && level == nativeLevel && result instanceof AbstractArrow arrow
                && shooter instanceof ServerPlayer actor && actor.level() == level && arrow.getOwner() == actor
                && actor.getItemInHand(hand) == crossbow && !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer)) {
            arrow.getPersistentData().putUUID("JemQuestNativeCrossbowActorV4", actor.getUUID());
        }
        return accepted;
    }
}
