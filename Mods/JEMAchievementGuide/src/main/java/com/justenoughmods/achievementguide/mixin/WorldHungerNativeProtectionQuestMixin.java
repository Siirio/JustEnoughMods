package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldHungerGuardProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.hook.handler.PlayerHandler", remap = false)
public abstract class WorldHungerNativeProtectionQuestMixin {
    @Inject(method = "isInvulnerableTo", at = @At("RETURN"))
    private static void jem$nativeBranch(Player actor, DamageSource source, CallbackInfoReturnable<Boolean> ci) { WorldHungerGuardProof.nativeProtection(actor, source, ci.getReturnValue()); }
}
