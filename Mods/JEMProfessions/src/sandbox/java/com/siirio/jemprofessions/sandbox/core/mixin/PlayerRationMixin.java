package com.siirio.jemprofessions.sandbox.core.mixin;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
public abstract class PlayerRationMixin {
    // Native checkMovementStatistics calls0..2 exhaust swimming; call3 alone exhausts sprinting.
    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V", ordinal = 3), index = 0)
    private float jem$sprint(float amount) { return SandboxCoreEffects.sprintExhaustion((Player) (Object) this, amount); }
    // Native jump call0 is sprint-jump; call1 is ordinary jump and remains unchanged.
    @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V", ordinal = 0), index = 0)
    private float jem$sprintJump(float amount) { return SandboxCoreEffects.sprintExhaustion((Player) (Object) this, amount); }
}
