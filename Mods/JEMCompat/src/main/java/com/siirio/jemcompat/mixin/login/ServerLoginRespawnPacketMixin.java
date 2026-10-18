package com.siirio.jemcompat.mixin.login;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A dead player must be able to reach the login UI through vanilla respawn. */
@Pseudo
@Mixin(targets = "ru.iwoss.serverlogin.gate.PacketPolicy", remap = false)
public abstract class ServerLoginRespawnPacketMixin {
    @Inject(method = "isAllowed(Lnet/minecraft/network/protocol/Packet;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$allowNativeRespawn(Packet<?> packet, CallbackInfoReturnable<Boolean> callback) {
        if (packet instanceof ServerboundClientCommandPacket command
                && command.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
            // Vanilla validates the player and binds the replacement to its connection.
            // This never unlocks the account; all other gate policy stays upstream-owned.
            callback.setReturnValue(true);
        }
    }
}
