package com.siirio.jemserver.mixin.teleport;

import com.siirio.jemserver.smp.events.BossTeleportSafety;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MobBoundaryTeleportMixin {
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void jem$nativePositionDestination(double x,double y,double z,CallbackInfo callback) {
        Entity entity=(Entity)(Object)this;
        if(!entity.isAddedToWorld() || !(entity.level() instanceof ServerLevel level)) return;
        Vec3 destination=new Vec3(x,y,z);
        BossTeleportSafety.Decision decision=BossTeleportSafety.validatePosition(entity,level,destination);
        if(decision.matched() && !destination.equals(decision.destination())) callback.cancel();
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void jem$boundaryDestination(double x, double y, double z, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level)) return;
        BossTeleportSafety.Decision decision = BossTeleportSafety.validate(entity, level, new Vec3(x, y, z));
        if (!decision.matched() || new Vec3(x, y, z).equals(decision.destination())) return;
        callback.cancel();
        if (entity instanceof net.minecraft.server.level.ServerPlayer player && decision.destination() != null)
            BossTeleportSafety.apply(player, level, decision.destination(), player.getYRot(), player.getXRot());
    }

    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$boundaryDimension(ServerLevel level, net.minecraftforge.common.util.ITeleporter teleporter, CallbackInfoReturnable<Entity> callback) {
        Entity entity = (Entity) (Object) this;
        if (!BossTeleportSafety.permitsDimensionChange(entity, level)) callback.setReturnValue(null);
    }

    @Redirect(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At(value="INVOKE", target="Lnet/minecraftforge/common/util/ITeleporter;getPortalInfo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Function;)Lnet/minecraft/world/level/portal/PortalInfo;",remap=false),remap=false)
    private net.minecraft.world.level.portal.PortalInfo jem$portalDestination(net.minecraftforge.common.util.ITeleporter teleporter,Entity entity,ServerLevel level,
            java.util.function.Function<ServerLevel,net.minecraft.world.level.portal.PortalInfo> fallback) {
        var info=teleporter.getPortalInfo(entity,level,fallback);
        if(info==null) return null;
        var decision=BossTeleportSafety.validate(entity,level,info.pos);
        return !decision.matched() || info.pos.equals(decision.destination()) ? info : null;
    }
}
