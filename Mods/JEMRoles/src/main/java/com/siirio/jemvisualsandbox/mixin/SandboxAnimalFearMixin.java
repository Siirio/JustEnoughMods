package com.siirio.jemvisualsandbox.mixin;

import com.siirio.jemvisualsandbox.workbench.SandboxRaceTraits;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The same narrow native fear seam as JEMRoles AvoidEntityGoalMixin. */
@Mixin(AvoidEntityGoal.class)
public abstract class SandboxAnimalFearMixin {
    @Shadow protected PathfinderMob mob;
    @Shadow protected LivingEntity toAvoid;
    @Inject(method = "canUse", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;toAvoid:Lnet/minecraft/world/entity/LivingEntity;",
            ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void jemSandbox$gentlePresence(CallbackInfoReturnable<Boolean> callback) {
        if (mob instanceof Animal && toAvoid instanceof ServerPlayer player && !mob.level().isClientSide
                && SandboxRaceTraits.hasRace(player, 0)) callback.setReturnValue(false);
    }
}
