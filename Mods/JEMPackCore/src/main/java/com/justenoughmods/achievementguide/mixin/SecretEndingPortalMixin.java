package com.justenoughmods.achievementguide.mixin;

import com.siirio.jempackcore.postend.PostEndEvents;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayer.class)
public abstract class SecretEndingPortalMixin {
    @ModifyArg(
            method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;<init>(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket$Type;F)V"),
            index = 1
    )
    private float jemcompat$startSecretEnding(float showCredits) {
        return PostEndEvents.startSecretEnding((ServerPlayer) (Object) this) ? 1.0F : showCredits;
    }
}
