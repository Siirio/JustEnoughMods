package com.siirio.jemcompat.mixin.login;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ordinary authenticated chat keeps vanilla recipients and sender identity. */
@Pseudo
@Mixin(targets = "ru.iwoss.serverlogin.chat.AdminChatManager", remap = false)
public abstract class ServerLoginPublicChatMixin {
    @Inject(method = "handleDefaultChat(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$publicChat(ServerPlayer player, String message, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
