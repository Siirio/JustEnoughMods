package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BowEffectProof;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.BlossomArrowEntity", remap = false)
public abstract class BlossomEffectQuestMixin {
    @Inject(method = "trySpawnStorm", at = @At("HEAD"))
    private void jem$begin(Vec3 position, LivingEntity target, CallbackInfo callback) { BowEffectProof.begin((Projectile) (Object) this); }
    @Inject(method = "trySpawnStorm", at = @At("RETURN"))
    private void jem$end(Vec3 position, LivingEntity target, CallbackInfo callback) { BowEffectProof.end(); }
}
