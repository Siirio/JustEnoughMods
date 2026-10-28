package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = DamageSources.class, remap = false)
public abstract class MagicLastDamageResultQuestMixin {
    @Inject(method = "applyDamage", at = @At("HEAD"))
    private static void jem$before(Entity target, float amount, DamageSource source, CallbackInfoReturnable<Boolean> callback) { MagicLastSpellProof.damageBegin(target, source); }
    @Inject(method = "applyDamage", at = @At("RETURN"))
    private static void jem$after(Entity target, float amount, DamageSource source, CallbackInfoReturnable<Boolean> callback) { MagicLastSpellProof.damageEnd(callback.getReturnValue()); }
}
