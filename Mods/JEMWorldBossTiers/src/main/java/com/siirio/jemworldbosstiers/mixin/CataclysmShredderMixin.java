package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.balance.ProgressionWeapons;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.Meat_Shredder", remap = false)
public abstract class CataclysmShredderMixin {
    // Cataclysm 3.31's primary continuous weapon action supplies its actual used stack.
    @Redirect(method = {"m_5929_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V",
            "onUseTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private boolean jem$primaryShredderHit(LivingEntity target, DamageSource source, float amount,
                                      Level level, LivingEntity wielder, ItemStack weapon, int remaining) {
        return ProgressionWeapons.nativeMeleeHit(target, source, amount, weapon);
    }
}
