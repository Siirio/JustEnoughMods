package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldOceanObservation;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WorldOceanMovementQuestMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void jem$acceptedObservation(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (awaitingPositionFromClient == null && player.connection == (Object)this) WorldOceanObservation.accepted(player);
    }
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$clearOnTeleport(CallbackInfo ci) { WorldOceanObservation.clear(player); }
}
