package com.siirio.jemserver.mixin.events;

import com.siirio.jemserver.smp.events.SolidBoundary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SolidBoundaryMovementValidationMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("RETURN"), cancellable = true)
    private void jem$solidCollisionShapes(LevelReader level, AABB previous, double x, double y, double z, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) return;
        AABB family = player.getRootVehicle().getSelfAndPassengers().map(net.minecraft.world.entity.Entity::getBoundingBox)
                .reduce(player.getBoundingBox(), AABB::minmax);
        AABB origin = family.move(previous.minX - player.getBoundingBox().minX,
                previous.minY - player.getBoundingBox().minY, previous.minZ - player.getBoundingBox().minZ);
        AABB destination = family.move(x - player.getX(), y - player.getY(), z - player.getZ());
        if (!SolidBoundary.permitsMovement(player, origin, destination)) callback.setReturnValue(true);
    }
}
