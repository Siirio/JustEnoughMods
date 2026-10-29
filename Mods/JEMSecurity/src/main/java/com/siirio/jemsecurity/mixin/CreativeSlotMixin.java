package com.siirio.jemsecurity.mixin;

import com.siirio.jemsecurity.ItemProvenance;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeSlotMixin {
    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"))
    private void jemSecurity$auditCreativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo callback) {
        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
        ItemProvenance.creativeSlot(listener.player, packet.getSlotNum(), packet.getItem());
    }
}
