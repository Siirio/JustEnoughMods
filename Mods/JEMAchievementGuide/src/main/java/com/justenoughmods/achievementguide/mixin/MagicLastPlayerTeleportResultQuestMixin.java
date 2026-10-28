package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastWorldProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MagicLastPlayerTeleportResultQuestMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "teleport(DDDFF)V", at = @At("HEAD"))
    private void jem$begin(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) { MagicLastWorldProof.moveBegin(player); }
    @Inject(method = "teleport(DDDFF)V", at = @At("RETURN"))
    private void jem$end(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) { MagicLastWorldProof.moveEnd(player); }
}
