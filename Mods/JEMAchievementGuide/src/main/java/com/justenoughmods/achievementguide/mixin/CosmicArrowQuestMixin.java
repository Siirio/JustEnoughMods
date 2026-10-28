package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BowEffectProof;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.CosmicArrowEntity", remap = false)
public abstract class CosmicArrowQuestMixin {
    @Inject(method = "m_5790_", at = @At("HEAD"))
    private void jem$begin(EntityHitResult hit, CallbackInfo callback) { BowEffectProof.begin((Projectile) (Object) this); }
    @Inject(method = "m_5790_", at = @At("RETURN"))
    private void jem$end(EntityHitResult hit, CallbackInfo callback) { BowEffectProof.end(); }
}
