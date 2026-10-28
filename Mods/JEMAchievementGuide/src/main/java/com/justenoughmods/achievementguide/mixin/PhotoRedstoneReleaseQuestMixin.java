package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoCameraProof;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import io.github.mortuusars.exposure.world.entity.CameraStandRedstoneControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CameraStandRedstoneControl.class, remap = false)
public abstract class PhotoRedstoneReleaseQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/github/mortuusars/exposure/world/entity/CameraStandEntity;release()V"))
    private void jem$nativeRelease(CameraStandEntity stand) {
        PhotoCameraProof.cause(PhotoCameraProof.Cause.REDSTONE);
        try { stand.release(); } finally { PhotoCameraProof.releaseDone(); }
    }
}
