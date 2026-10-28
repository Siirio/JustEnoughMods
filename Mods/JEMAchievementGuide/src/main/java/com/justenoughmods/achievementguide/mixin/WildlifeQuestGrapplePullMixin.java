package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple", remap = false)
public abstract class WildlifeQuestGrapplePullMixin implements WildlifeQuestMotion.NativeForce {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))
    private void jem$nativePull(Entity owner, Vec3 motion) {
        Vec3 before = owner.getDeltaMovement(); owner.setDeltaMovement(motion); var self = (EntitySquidGrapple)(Object)this;
        if (owner instanceof ServerPlayer actor && WildlifeQuestProof.real(actor) != null && jem$wildlifeForceActive(actor)
                && WildlifeQuestProof.matches(self, "grapple_native_actor", actor.getUUID()) && WildlifeQuestProof.data(self).hasUUID("grapple_tool")) {
            WildlifeQuestMotion.force(actor, self, "grapple", motion.subtract(before), WildlifeQuestProof.data(self).getUUID("grapple_tool"));
        }
    }
    @Override public boolean jem$wildlifeForceActive(ServerPlayer actor) {
        var self = (EntitySquidGrapple)(Object)this;
        return !self.isRemoved() && self.getOwner() == actor && self.getStuckToPos() != null && !self.isWithdrawing() && !self.level().getBlockState(self.getStuckToPos()).isAir();
    }
}
