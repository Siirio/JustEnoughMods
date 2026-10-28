package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelThermalObservation;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.api.movement.Movement;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ParagliderTravelPacketQuestMixin {
    @Shadow public ServerPlayer player;
    @Unique private Vec3 jem$beforeThermalMove;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void jem$beforeThermalMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        jem$beforeThermalMove = player.position();
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setOnGroundWithKnownMovement(ZLnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.AFTER))
    private void jem$acceptedThermalMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (jem$beforeThermalMove != null && Movement.get(player) instanceof TravelThermalObservation observation) {
            observation.jem$thermalAccepted(jem$beforeThermalMove);
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void jem$clearThermalMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) { jem$beforeThermalMove = null; }
}
