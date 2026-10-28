package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareOutputs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class CreatureCareNativeInsertionMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$inserted(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        CreatureCareOutputs.inserted(entity, callback.getReturnValue());
    }
}
