package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoCameraProof;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CameraItem.class, remap = false)
public abstract class PhotoCaptureOwnerQuestMixin {
    @Inject(method = "takePhoto", at = @At("HEAD"))
    private void jem$before(CameraHolder holder, ServerPlayer executor, ItemStack stack, CallbackInfo callback) { PhotoCameraProof.shot(holder, executor, stack); }
    @Inject(method = "takePhoto", at = @At("RETURN"))
    private void jem$after(CameraHolder holder, ServerPlayer executor, ItemStack stack, CallbackInfo callback) { PhotoCameraProof.shotDone(); }
}
