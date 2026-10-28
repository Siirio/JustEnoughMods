package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
public abstract class BattleQuestAcceptedMoveMixin {
    @Shadow public net.minecraft.server.level.ServerPlayer player;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
    @Unique private boolean jem$accepted;
    @Unique private net.minecraft.world.phys.Vec3 jem$before;
    @Inject(method="handleMovePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER)) private void jem$before(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,CallbackInfo ci) { jem$before=player.position();jem$accepted=false; }
    @Inject(method="handleMovePlayer",at=@At(value="FIELD",target="Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D",opcode=org.objectweb.asm.Opcodes.PUTFIELD,shift=At.Shift.AFTER)) private void jem$nativeAccepted(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,CallbackInfo ci){jem$accepted=awaitingPositionFromClient==null&&player.connection==(Object)this;}
    @Inject(method="handleMovePlayer",at=@At("RETURN")) private void jem$after(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,CallbackInfo ci) { if(jem$before!=null&&jem$accepted&&awaitingPositionFromClient==null) BattleQuestEpic.moved(player,jem$before); jem$before=null; }
}
