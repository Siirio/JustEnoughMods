package com.justenoughmods.achievementguide.mixin;

import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.recrafted.blastfromthepast.network.ScreenShakePayload;
import team.recrafted.blastfromthepast.network.handlers.ClientPayloadHandler;

/** Disables only Blast From The Past camera shake while preserving its combat and sound packets. */
@Mixin(value = ClientPayloadHandler.class, remap = false)
public abstract class BlastFromThePastScreenShakeMixin {
    @Inject(method = "handleScreenShake", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$discardShake(ScreenShakePayload payload,
                                         Supplier<NetworkEvent.Context> context,
                                         CallbackInfo callback) {
        context.get().setPacketHandled(true);
        callback.cancel();
    }
}
