package com.siirio.jemserver.mixin.teleport;

import com.siirio.jemserver.smp.events.BossTeleportSafety;
import com.siirio.jemserver.smp.events.BloodMoonSolidBoundary;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"),cancellable=true)
    private void jem$validateCommandArenaDestination(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relativeMovements,float yaw,float pitch,CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        Vec3 requested=new Vec3(x,y,z);
        BossTeleportSafety.Decision decision=BossTeleportSafety.validate(player,level,requested);
        if(!decision.matched()) decision=BloodMoonSolidBoundary.validateTeleport(player,level,requested);
        if(!decision.matched()||requested.equals(decision.destination())) return;
        if(decision.destination()!=null) BossTeleportSafety.apply(player,level,decision.destination(),yaw,pitch);
        callback.setReturnValue(decision.destination()!=null);
    }

    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"),cancellable=true)
    private void jem$validateArenaDestination(ServerLevel level,double x,double y,double z,float yaw,float pitch,CallbackInfo callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        Vec3 requested=new Vec3(x,y,z);
        BossTeleportSafety.Decision decision=BossTeleportSafety.validate(player,level,requested);
        if(!decision.matched()) decision=BloodMoonSolidBoundary.validateTeleport(player,level,requested);
        if(!decision.matched()||requested.equals(decision.destination())) return;
        callback.cancel();
        if(decision.destination()!=null) BossTeleportSafety.apply(player,level,decision.destination(),yaw,pitch);
    }
}
