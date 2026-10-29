package com.siirio.jemcompat.mixin.login;

import io.netty.channel.ChannelHandlerContext;
import java.nio.channels.ClosedChannelException;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ServerLogin 1.0.3 reports every failed queued write before vanilla's fault guard. */
@Mixin(value=Connection.class,priority=2100)
public abstract class ClosedConnectionErrorMixin {
    @Unique private boolean jem$closedChannelReported;

    @Inject(method="exceptionCaught",at=@At("HEAD"),cancellable=true)
    private void jem$closedChannelOnce(ChannelHandlerContext context,Throwable failure,CallbackInfo callback) {
        if (!(failure instanceof ClosedChannelException)) return;
        if (jem$closedChannelReported) callback.cancel();
        else jem$closedChannelReported=true;
    }
}
