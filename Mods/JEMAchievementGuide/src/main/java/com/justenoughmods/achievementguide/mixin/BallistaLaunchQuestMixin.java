package com.justenoughmods.achievementguide.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.talhanation.siegeweapons.entities.BallistaEntity", remap = false)
public abstract class BallistaLaunchQuestMixin {
    @Unique private int jem$ammoBefore;
    @Unique private boolean jem$nativeLaunch;

    @Inject(method = "loadProjectile", at = @At("HEAD"))
    private void jem$beforeLoad(ItemStack ammunition, CallbackInfoReturnable<Boolean> callback) {
        jem$ammoBefore = ammunition.getCount();
    }

    @Inject(method = "loadProjectile", at = @At("RETURN"))
    private void jem$afterLoad(ItemStack ammunition, CallbackInfoReturnable<Boolean> callback) {
        Entity weapon = (Entity) (Object) this;
        if (!weapon.level().isClientSide && Boolean.TRUE.equals(callback.getReturnValue())
                && ammunition.getCount() == jem$ammoBefore - 1) {
            weapon.getPersistentData().putBoolean("JemQuestPaidBallistaAmmoV4", true);
        }
    }

    // This call exists only in the native PROJECTILE_LOADED tick branch after state becomes UNLOADED
    @Inject(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lcom/talhanation/siegeweapons/entities/BallistaEntity;shootWeapon()V"))
    private void jem$beginNativeLaunch(CallbackInfo callback) {
        jem$nativeLaunch = ((Entity) (Object) this).getPersistentData().getBoolean("JemQuestPaidBallistaAmmoV4");
    }

    @Inject(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lcom/talhanation/siegeweapons/entities/BallistaEntity;shootWeapon()V", shift = At.Shift.AFTER))
    private void jem$finishNativeLaunch(CallbackInfo callback) {
        jem$nativeLaunch = false;
        ((Entity) (Object) this).getPersistentData().remove("JemQuestPaidBallistaAmmoV4");
    }

    @Redirect(method = "shoot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$acceptedSiegeLaunch(Level level, Entity projectile) {
        boolean spawned = level.addFreshEntity(projectile);
        if (spawned && jem$nativeLaunch && projectile instanceof AbstractArrow arrow
                && arrow.getOwner() instanceof ServerPlayer actor && !actor.getAbilities().instabuild
                && !actor.isSpectator() && !(actor instanceof net.minecraftforge.common.util.FakePlayer)) {
            projectile.getPersistentData().putUUID("JemQuestNativeSiegeActorV4", actor.getUUID());
        }
        return spawned;
    }
}
