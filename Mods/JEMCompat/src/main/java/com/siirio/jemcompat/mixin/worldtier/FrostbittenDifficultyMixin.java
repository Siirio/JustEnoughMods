package com.siirio.jemcompat.mixin.worldtier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Corrects the reversed difficulty offsets in Legendary Monsters 2.2.2 only. */
@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Frostbitten_GolemEntity", remap = false)
public abstract class FrostbittenDifficultyMixin {
    // Native HARD subtracts this offset after multiplying the configured base.
    @ModifyConstant(method = "attackTargets(Lnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(doubleValue = 4.0D), require = 1, expect = 1)
    private double jemcompat$hardDamageOffset(double nativeOffset) {
        return -4.0D;
    }

    // Native EASY adds this offset. NORMAL and the native config multiplier stay intact.
    @ModifyConstant(method = "attackTargets(Lnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(doubleValue = 13.0D), require = 1, expect = 1)
    private double jemcompat$easyDamageOffset(double nativeOffset) {
        return -4.0D;
    }
}
