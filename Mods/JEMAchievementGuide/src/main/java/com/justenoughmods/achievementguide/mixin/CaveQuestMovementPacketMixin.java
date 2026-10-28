package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.caves.CaveQuestMotion;
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

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CaveQuestMovementPacketMixin {
    @Shadow public ServerPlayer player;
    @Unique private Vec3 jem$beforeCaveMovement;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void jem$beforeCaveMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) { jem$beforeCaveMovement = player.position(); }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setOnGroundWithKnownMovement(ZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void jem$acceptedCaveMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (jem$beforeCaveMovement != null) CaveQuestMotion.of(player).moved(player, jem$beforeCaveMovement);
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void jem$clearCaveMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) { jem$beforeCaveMovement = null; }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$resetCaveMotion(CallbackInfo callback) {
        jem$beforeCaveMovement = null;
        CaveQuestMotion.of(player).clear();
    }
}
