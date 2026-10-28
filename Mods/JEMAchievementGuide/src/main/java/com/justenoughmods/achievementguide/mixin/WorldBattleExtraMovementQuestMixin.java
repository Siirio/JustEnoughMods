package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraMovement;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WorldBattleExtraMovementQuestMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Unique private Vec3 jem$beforeBattleMove;
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void jem$begin(ServerboundMovePlayerPacket packet, CallbackInfo ci) { jem$beforeBattleMove = player.position(); }
    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void jem$accepted(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (jem$beforeBattleMove != null && awaitingPositionFromClient == null && player.connection == (Object)this)
            WorldBattleExtraMovement.accepted(player, jem$beforeBattleMove);
        jem$beforeBattleMove = null;
    }
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void jem$end(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        jem$beforeBattleMove = null;
        if (awaitingPositionFromClient != null) WorldBattleExtraMovement.interrupted(player);
    }
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$teleport(CallbackInfo ci) { jem$beforeBattleMove = null; WorldBattleExtraMovement.interrupted(player); }
}
