package com.siirio.jemserver.mixin.teleport;

import com.siirio.jemserver.smp.events.BossTeleportSafety;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {
    @Shadow protected abstract net.minecraft.world.level.portal.PortalInfo findDimensionEntryPoint(ServerLevel level);
    @Unique private net.minecraft.world.level.portal.PortalInfo jem$approvedPortalInfo;

    @Inject(method="changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",at=@At("HEAD"),cancellable=true,remap=false)
    private void jem$preflightPortal(ServerLevel level,net.minecraftforge.common.util.ITeleporter teleporter,CallbackInfoReturnable<net.minecraft.world.entity.Entity> callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        jem$approvedPortalInfo=null;
        if(!BossTeleportSafety.permitsDimensionChange(player,level)) { callback.setReturnValue(null); return; }
        if(player.level().dimension()==net.minecraft.world.level.Level.END && level.dimension()==net.minecraft.world.level.Level.OVERWORLD && teleporter.isVanilla()) return;
        var info=teleporter.getPortalInfo(player,level,this::findDimensionEntryPoint);
        if(info==null) { callback.setReturnValue(null); return; }
        var decision=BossTeleportSafety.validate(player,level,info.pos);
        if(decision.matched() && !info.pos.equals(decision.destination())) { callback.setReturnValue(null); return; }
        jem$approvedPortalInfo=info;
    }

    @Redirect(method="changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at=@At(value="INVOKE",target="Lnet/minecraftforge/common/util/ITeleporter;getPortalInfo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Function;)Lnet/minecraft/world/level/portal/PortalInfo;",remap=false),remap=false)
    private net.minecraft.world.level.portal.PortalInfo jem$approvedPortal(net.minecraftforge.common.util.ITeleporter teleporter,net.minecraft.world.entity.Entity entity,ServerLevel level,
            java.util.function.Function<ServerLevel,net.minecraft.world.level.portal.PortalInfo> fallback) {
        var info=jem$approvedPortalInfo;
        jem$approvedPortalInfo=null;
        return info;
    }

    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"),cancellable=true)
    private void jem$validateCommandArenaDestination(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relativeMovements,float yaw,float pitch,CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        Vec3 requested=new Vec3(x,y,z);
        BossTeleportSafety.Decision decision=BossTeleportSafety.validate(player,level,requested);
        if(!decision.matched()||requested.equals(decision.destination())) return;
        if(decision.destination()!=null) BossTeleportSafety.apply(player,level,decision.destination(),yaw,pitch);
        callback.setReturnValue(decision.destination()!=null);
    }

    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"),cancellable=true)
    private void jem$validateArenaDestination(ServerLevel level,double x,double y,double z,float yaw,float pitch,CallbackInfo callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        Vec3 requested=new Vec3(x,y,z);
        BossTeleportSafety.Decision decision=BossTeleportSafety.validate(player,level,requested);
        if(!decision.matched()||requested.equals(decision.destination())) return;
        callback.cancel();
        if(decision.destination()!=null) BossTeleportSafety.apply(player,level,decision.destination(),yaw,pitch);
    }
}
