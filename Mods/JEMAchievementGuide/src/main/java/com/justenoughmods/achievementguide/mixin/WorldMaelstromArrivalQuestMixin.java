package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldMaelstromProof;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.effect.MaelstromOmenEffect", remap = false)
public abstract class WorldMaelstromArrivalQuestMixin {
    @Inject(method = "changeDimension", at = @At(value = "INVOKE", target = "Ldev/obscuria/aquamirae/api/common/helper/PlayerHelper;setTeleportProgress(Lnet/minecraft/world/entity/player/Player;F)V"))
    private void jem$nativeArrival(ServerPlayer actor, ServerLevel source, ResourceKey<Level> destination, Vec3 origin, CallbackInfo ci) {
        WorldMaelstromProof.arrived(actor, source, destination);
    }
}
