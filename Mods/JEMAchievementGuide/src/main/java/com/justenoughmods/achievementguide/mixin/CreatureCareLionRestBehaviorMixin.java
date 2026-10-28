package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.entity.ai.behavior.lion.AnimalRelaxOnSleepingOwner", remap = false)
public abstract class CreatureCareLionRestBehaviorMixin {
    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/TamableAnimal;J)V", at = @At("RETURN"))
    private void jem$rest(ServerLevel level, TamableAnimal mob, long time, CallbackInfo callback) {
        if (CreatureCareProof.type(mob).equals("primal:lion") && mob.getOwnerUUID() != null) {
            CreatureCareProof.mark(mob, "lion_native_rest", mob.getOwnerUUID());
        }
    }
    @Inject(method = "stop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/TamableAnimal;J)V", at = @At("HEAD"))
    private void jem$stop(ServerLevel level, TamableAnimal mob, long time, CallbackInfo callback) {
        CreatureCareProof.data(mob).remove("lion_native_rest");
    }
}
