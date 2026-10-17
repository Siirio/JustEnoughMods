package com.siirio.jemprofessions.sandbox.core.mixin;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerTeleportMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void jem$teleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relatives, CallbackInfo callback) { SandboxCoreEffects.teleported(player); }
}
