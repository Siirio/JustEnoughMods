package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.primal.entity.animal.LionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.LionEntity", remap = false)
public abstract class CreatureCareLionRestPoseMixin {
    @Inject(method = "m_8024_", at = @At("RETURN"))
    private void jem$pose(CallbackInfo callback) {
        LionEntity self = (LionEntity)(Object)this;
        var actor = CreatureCareProof.actor(self, "lion_meat_owner");
        if (CreatureCareProof.owned(self, actor) && actor.isSleeping() && self.isLaying() && self.getPose() == Pose.DIGGING
                && CreatureCareProof.matches(self, "lion_native_rest", actor.getUUID())
                && self.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).orElse(null) instanceof EntityTracker tracker && tracker.getEntity() == actor) {
            CreatureCareProof.emit(actor, "v3_fauna_lion_trust");
        }
    }
}
