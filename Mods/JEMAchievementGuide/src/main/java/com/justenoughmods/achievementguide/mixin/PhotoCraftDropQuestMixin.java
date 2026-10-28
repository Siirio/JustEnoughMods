package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoCraftProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class PhotoCraftDropQuestMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$nativeOutput(Entity entity, CallbackInfoReturnable<Boolean> callback) { PhotoCraftProof.inserted(entity, callback.getReturnValue()); }
}
