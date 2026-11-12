package dev.taste.engine.mixin.client;

import dev.taste.engine.client.ClientEvents;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPlacementPacketMixin {
    @Inject(method = "handleBlockUpdate(Lnet/minecraft/network/protocol/game/ClientboundBlockUpdatePacket;)V",
            at = @At("RETURN"))
    private void taste$confirmPlacement(ClientboundBlockUpdatePacket packet, CallbackInfo callback) {
        ClientEvents.confirmPlacement(packet.getPos(), packet.getBlockState());
    }
}
