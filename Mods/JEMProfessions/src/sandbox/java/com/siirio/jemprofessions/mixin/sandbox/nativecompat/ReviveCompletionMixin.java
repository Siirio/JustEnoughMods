package com.siirio.jemprofessions.mixin.sandbox.nativecompat;

import com.siirio.jemprofessions.sandbox.nativecompat.SandboxReviveEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.playerrevive.server.PlayerReviveServer;

@Mixin(value = PlayerReviveServer.class, remap = false)
public abstract class ReviveCompletionMixin {
    @Inject(method = "revive", at = @At("HEAD"))
    private static void jem$helpers(Player player, CallbackInfo callback) { SandboxReviveEffects.beginning(player); }
}
