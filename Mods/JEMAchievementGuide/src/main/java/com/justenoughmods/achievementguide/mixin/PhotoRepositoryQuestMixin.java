package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoImages;
import io.github.mortuusars.exposure.world.level.storage.ExposureRepository;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExposureRepository.class, remap = false)
public abstract class PhotoRepositoryQuestMixin {
    @Inject(method = "onExposureReceived", at = @At("RETURN"))
    private void jem$stored(ServerPlayer uploader, String id, CallbackInfo callback) { PhotoImages.received(uploader, id); }
}
