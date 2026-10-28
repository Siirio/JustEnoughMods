package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRides;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WildlifeQuestAcceptedRideMixin {
    @Shadow public ServerPlayer player;
    @Unique private WildlifeQuestRides.Move jem$ride;
    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void jem$before(ServerboundMoveVehiclePacket packet, CallbackInfo callback) { jem$ride = WildlifeQuestRides.begin(player); }
    @Inject(method = "handleMoveVehicle", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;vehicleLastGoodZ:D", opcode = 181, shift = At.Shift.AFTER))
    private void jem$accepted(ServerboundMoveVehiclePacket packet, CallbackInfo callback) { var run = jem$ride; jem$ride = null; WildlifeQuestRides.accepted(run); }
}
