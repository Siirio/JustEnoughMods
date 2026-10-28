package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoCameraProof;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.item.camera.Timer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Timer.class, remap = false)
public abstract class PhotoTimerReleaseQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/github/mortuusars/exposure/world/item/camera/CameraItem;release(Lio/github/mortuusars/exposure/world/entity/CameraHolder;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/InteractionResultHolder;"))
    private InteractionResultHolder<ItemStack> jem$nativeRelease(CameraItem camera, CameraHolder holder, ItemStack stack) {
        PhotoCameraProof.cause(PhotoCameraProof.Cause.TIMER);
        try { return camera.release(holder, stack); } finally { PhotoCameraProof.releaseDone(); }
    }
}
