package dev.taste.engine.mixin.client;

import dev.taste.engine.client.ClientEvents;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientExplosionMixin {
    @Inject(method = "handleExplosion(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V", at = @At("RETURN"))
    private void taste$recordExplosion(ClientboundExplodePacket packet, CallbackInfo callback) {
        ClientEvents.onExplosion(packet.getX(), packet.getY(), packet.getZ(), packet.getPower());
    }
}
