package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldHungerGuardProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class WorldHungerDamageQuestMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$attempt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) { WorldHungerGuardProof.begin((Player)(Object)this, source, amount); }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$rejected(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) { WorldHungerGuardProof.end((Player)(Object)this, source, ci.getReturnValue()); }
}
