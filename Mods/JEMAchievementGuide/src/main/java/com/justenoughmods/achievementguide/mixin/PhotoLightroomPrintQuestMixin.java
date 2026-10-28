package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoLightroomState;
import com.justenoughmods.achievementguide.integration.photo.PhotoPrintingProof;
import io.github.mortuusars.exposure.world.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.lightroom.PrintingProcess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightroomBlockEntity.class, remap = false)
public abstract class PhotoLightroomPrintQuestMixin implements PhotoLightroomState {
    @Shadow protected int printTime;
    @Shadow protected abstract PrintingProcess getPrintingProcess(Frame frame);
    @Override public int jem$photoPrintTime() { return printTime; }
    @Override public PrintingProcess jem$photoProcess(Frame frame) { return getPrintingProcess(frame); }

    @Inject(method = "startPrintingProcess", at = @At("RETURN"))
    private void jem$started(boolean advance, CallbackInfo callback) { PhotoPrintingProof.started((LightroomBlockEntity)(Object)this); }
    @Inject(method = "stopPrintingProcess", at = @At("HEAD"))
    private void jem$stopped(CallbackInfo callback) { PhotoPrintingProof.stopped((LightroomBlockEntity)(Object)this); }
    @Inject(method = "printFrame", at = @At("HEAD"))
    private void jem$before(Frame frame, PrintingProcess process, boolean consume, CallbackInfo callback) {
        PhotoPrintingProof.before((LightroomBlockEntity)(Object)this, frame, process, consume);
    }
    @Inject(method = "onFramePrinted", at = @At("HEAD"))
    private void jem$nativeResult(Frame frame, PrintingProcess process, ItemStack result, CallbackInfo callback) {
        PhotoPrintingProof.nativeResult((LightroomBlockEntity)(Object)this, frame, process, result);
    }
    @Inject(method = "printFrame", at = @At("RETURN"))
    private void jem$after(Frame frame, PrintingProcess process, boolean consume, CallbackInfo callback) {
        PhotoPrintingProof.after((LightroomBlockEntity)(Object)this);
    }
}
