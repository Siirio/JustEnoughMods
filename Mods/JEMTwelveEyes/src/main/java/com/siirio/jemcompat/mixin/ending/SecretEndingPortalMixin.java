package com.siirio.jemcompat.mixin.ending;

import com.siirio.jemtwelveeyes.postend.PostEndEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class SecretEndingPortalMixin {
    @Inject(
            method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void jemcompat$startSecretEnding(ServerLevel destination, ITeleporter teleporter,
                                             CallbackInfoReturnable<Entity> callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().dimension() != Level.END || destination.dimension() != Level.OVERWORLD
                || !PostEndEvents.startSecretEnding(player)) return;
        PostEndEvents.holdAtEndPortal(player);
        callback.setReturnValue(player);
    }
}
