package com.siirio.jemcompat.mixin.login;

import com.siirio.jemcompat.compat.login.TerminalSocketFailure;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The early blocker retains its decoder-error policy; terminal socket IO just closes. */
@Pseudo
@Mixin(targets = "com.connectivity.mixin.malformedtraffic.ChannelHandler$1", remap = false)
public abstract class ConnectivityTerminalSocketMixin {
    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$excludeTransportTermination(ChannelHandlerContext context, Throwable failure, CallbackInfo callback) {
        if (!TerminalSocketFailure.matches(failure)) return;
        context.close();
        callback.cancel();
    }
}
