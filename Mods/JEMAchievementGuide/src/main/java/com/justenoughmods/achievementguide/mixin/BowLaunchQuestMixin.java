package com.justenoughmods.achievementguide.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowItem.class)
public abstract class BowLaunchQuestMixin {
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$acceptedBowLaunch(Level level, Entity projectile, ItemStack bow, Level useLevel,
                                          LivingEntity shooter, int remainingUseTicks) {
        boolean spawned = level.addFreshEntity(projectile);
        if (spawned && projectile instanceof AbstractArrow arrow && shooter instanceof ServerPlayer actor
                && arrow.getOwner() == actor && !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer)) {
            arrow.getPersistentData().putUUID("JemQuestNativeBowActorV4", actor.getUUID());
        }
        return spawned;
    }
}
