package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball", remap = false)
public abstract class MagicLastFireballImpactQuestMixin {
    @Inject(method = "m_6532_", at = @At("HEAD"))
    private void jem$begin(HitResult hit, CallbackInfo callback) { MagicLastSpellProof.nativeBegin((Projectile)(Object)this); }
    @Inject(method = "m_6532_", at = @At("RETURN"))
    private void jem$end(HitResult hit, CallbackInfo callback) { MagicLastSpellProof.nativeEnd(); }
}
