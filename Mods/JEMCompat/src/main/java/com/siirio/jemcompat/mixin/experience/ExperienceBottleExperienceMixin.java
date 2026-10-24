package com.siirio.jemcompat.mixin.experience;

import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scale only the native bottle roll immediately before its server-owned orb award. */
@Mixin(ThrownExperienceBottle.class)
public abstract class ExperienceBottleExperienceMixin {
    @ModifyArg(
            method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
            index = 2
    )
    private int jemcompat$tripleBottleExperience(int nativeExperience) {
        return ((ThrownExperienceBottle) (Object) this).getItem().is(Items.EXPERIENCE_BOTTLE)
                ? nativeExperience * 3 : nativeExperience;
    }
}
