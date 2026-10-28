package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.beach.BeachTraversalProof;
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
public abstract class BeachAcceptedMovementQuestMixin implements BeachTraversalProof.Access {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Unique private final BeachTraversalProof jem$traversal = new BeachTraversalProof();
    @Unique private Vec3 jem$beforeNativeMove;

    @Override
    public BeachTraversalProof jem$nativeBeachTraversal() { return jem$traversal; }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void jem$serverMoveStart(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        jem$traversal.expire(player);
        jem$beforeNativeMove = player.position();
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER))
    private void jem$acceptedMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        Vec3 before = jem$beforeNativeMove;
        jem$beforeNativeMove = null;
        if (before != null && awaitingPositionFromClient == null && player.connection == (Object)this) {
            jem$traversal.acceptedMovement(player, before);
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void jem$moveEnd(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        jem$beforeNativeMove = null;
        if (awaitingPositionFromClient != null) jem$traversal.clear();
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$teleportInvalidates(CallbackInfo callback) {
        jem$beforeNativeMove = null;
        jem$traversal.clear();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$expireObservations(CallbackInfo callback) {
        jem$beforeNativeMove = null;
        jem$traversal.expire(player);
    }
}
