package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PetUtilityQuestVehiclePacketMixin {
 @Shadow public ServerPlayer player;
 @Unique private Entity jem$vehicle;
 @Unique private Vec3 jem$from;
 @Inject(method="handleMoveVehicle",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER))private void jem$before(ServerboundMoveVehiclePacket packet,CallbackInfo ci){jem$vehicle=PetUtilityQuestProof.actor(player)==null?null:player.getRootVehicle();jem$from=jem$vehicle==null?null:jem$vehicle.position();}
 @Inject(method="handleMoveVehicle",at=@At("RETURN"))private void jem$accepted(ServerboundMoveVehiclePacket packet,CallbackInfo ci){var e=jem$vehicle;var from=jem$from;jem$vehicle=null;jem$from=null;if(e!=null && e==player.getRootVehicle() && e.getControllingPassenger()==player && from!=null && from.distanceToSqr(e.position())>1E-8 && Math.abs(e.getX()-packet.getX())<1E-7 && Math.abs(e.getY()-packet.getY())<1E-7 && Math.abs(e.getZ()-packet.getZ())<1E-7 && e instanceof PetUtilityQuestVehicle observer)observer.jem$petAcceptedMotion(player,from);}
}
