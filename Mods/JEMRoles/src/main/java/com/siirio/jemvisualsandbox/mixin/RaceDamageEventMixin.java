package com.siirio.jemvisualsandbox.mixin;

import com.siirio.jemvisualsandbox.client.races.RaceDamagePresentation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress only the server-marked racial drowning hit, after thread dispatch. */
@Mixin(ClientPacketListener.class)
public abstract class RaceDamageEventMixin {
    @Shadow private ClientLevel level;

    @Inject(method="handleDamageEvent",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void jem$quietRaceDrowning(ClientboundDamageEventPacket packet,CallbackInfo callback) {
        if(RaceDamagePresentation.shouldSuppress(packet,level))callback.cancel();
    }
}
