package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WildlifeQuestAcceptedMovementMixin {
    @Shadow public ServerPlayer player;
    @Unique private WildlifeQuestMotion.Move jem$move;
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void jem$before(ServerboundMovePlayerPacket packet, CallbackInfo callback) { jem$move = WildlifeQuestProof.real(player) == null ? null : new WildlifeQuestMotion.Move(player, player.position()); }
    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", opcode = 181, shift = At.Shift.AFTER))
    private void jem$accepted(ServerboundMovePlayerPacket packet, CallbackInfo callback) { var run = jem$move; jem$move = null; WildlifeQuestMotion.accepted(run); }
}
