package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.integration.travel.TravelVehicleObservation;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TravelVehiclePacketQuestMixin {
    @Shadow public ServerPlayer player;
    @Unique private Entity jem$travelVehicle;
    @Unique private Vec3 jem$travelFrom;
    @Unique private boolean jem$travelGrounded;

    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void jem$beforeAcceptedMove(ServerboundMoveVehiclePacket packet, CallbackInfo callback) {
        jem$travelVehicle = player.getRootVehicle();
        jem$travelFrom = jem$travelVehicle.position();
        jem$travelGrounded = jem$travelVehicle.onGround();
    }

    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerChunkCache;move(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void jem$acceptedMove(ServerboundMoveVehiclePacket packet, CallbackInfo callback) {
        Entity vehicle = jem$travelVehicle;
        if (!(vehicle instanceof TravelVehicleObservation observation) || jem$travelFrom == null) return;
        // Vanilla's movement admission bound also applies to proof for the exempt singleplayer owner.
        if (TravelProof.controller(vehicle) != player || !TravelProof.finite(jem$travelFrom)
                || !TravelProof.finite(vehicle.position())
                || vehicle.position().distanceToSqr(jem$travelFrom) - vehicle.getDeltaMovement().lengthSqr() > 100.0) {
            observation.jem$travelReset();
            return;
        }
        observation.jem$travelAccepted(player, jem$travelFrom, jem$travelGrounded);
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void jem$clearMove(ServerboundMoveVehiclePacket packet, CallbackInfo callback) {
        jem$travelVehicle = null;
        jem$travelFrom = null;
    }
}
