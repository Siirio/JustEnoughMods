package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.siirio.jemcompat.feature.backpack.WorkbenchAugment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.stream.Stream;

@Mixin(value = AugmentType.class, remap = false)
public abstract class BackpackIntegratedAugmentListMixin {
    @Inject(method = "stream()Ljava/util/stream/Stream;", at = @At("RETURN"), cancellable = true, remap = false)
    private static void jemcompat$hideServerWorkbench(CallbackInfoReturnable<Stream<AugmentType<? extends Augment<?>>>> callback) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.isDedicatedServer()) {
            callback.setReturnValue(callback.getReturnValue().filter(type -> type != WorkbenchAugment.TYPE));
        }
    }
}
