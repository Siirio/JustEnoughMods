package com.siirio.jemcompat.mixin.login;

import com.siirio.jemcompat.compat.login.TerminalSocketFailure;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Close an already broken transport before Connectivity counts disconnect-write failures. */
@Mixin(value = Connection.class, priority = 2000)
public abstract class TerminalConnectionFailureMixin {
    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void jem$closeTerminatedTransport(ChannelHandlerContext context, Throwable failure, CallbackInfo callback) {
        if (!TerminalSocketFailure.matches(failure)) return;
        context.close();
        callback.cancel();
    }
}
