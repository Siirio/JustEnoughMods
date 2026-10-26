package com.siirio.jemcompat.mixin.login;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ordinary authenticated chat keeps vanilla recipients and sender identity. */
@Pseudo
@Mixin(targets = "ru.iwoss.serverlogin.event.LoginEvents", remap = false)
public abstract class ServerLoginPublicChatMixin {
    @Redirect(method = "onChat", at = @At(value = "INVOKE",
            target = "Lru/iwoss/serverlogin/chat/AdminChatManager;handleDefaultChat(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)Z"),
            remap = false, require = 1, expect = 1, allow = 1)
    private boolean jem$publicChat(@Coerce Object adminChat, ServerPlayer player, String message) {
        return false;
    }
}
