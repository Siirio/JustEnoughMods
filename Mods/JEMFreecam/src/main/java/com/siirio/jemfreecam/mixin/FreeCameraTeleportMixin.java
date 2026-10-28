package com.siirio.jemfreecam.mixin;

import com.siirio.jemfreecam.client.FreeCameraController;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Close only after the server position/rotation correction reached the client thread. */
@Mixin(ClientPacketListener.class)
public abstract class FreeCameraTeleportMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void jemcompat$serverTeleportHasPriority(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        FreeCameraController.onServerTeleport();
    }
}
