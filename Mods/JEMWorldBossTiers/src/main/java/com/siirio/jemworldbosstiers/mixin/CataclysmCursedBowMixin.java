package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.balance.ProgressionWeapons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.Cursed_bow", remap = false)
public abstract class CataclysmCursedBowMixin {
    // Cataclysm 3.31 owns its releaseUsing implementation instead of extending BowItem.
    @Redirect(method = {"m_5551_", "releaseUsing"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V", remap = true),
            require = 1, expect = 1, remap = false)
    private void jem$captureCursedBow(AbstractArrow arrow, Entity entity, float pitch, float yaw, float offset,
                                      float velocity, float inaccuracy, ItemStack weapon, Level level,
                                      LivingEntity shooter, int remaining) {
        ProgressionWeapons.captureLaunch(arrow, weapon, shooter);
        arrow.shootFromRotation(entity, pitch, yaw, offset, velocity, inaccuracy);
    }
}
