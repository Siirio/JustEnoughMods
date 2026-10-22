package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.balance.ProgressionWeapons;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class ProgressionCrossbowMixin {
    @Inject(method = "getArrow", at = @At("RETURN"), require = 1, expect = 1)
    private static void jem$captureCrossbow(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo,
                                            CallbackInfoReturnable<AbstractArrow> cir) {
        ProgressionWeapons.captureLaunch(cir.getReturnValue(), weapon, shooter);
    }
}
