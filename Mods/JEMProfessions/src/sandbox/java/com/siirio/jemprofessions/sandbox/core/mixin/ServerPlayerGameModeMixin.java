package com.siirio.jemprofessions.sandbox.core.mixin;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private SandboxCoreEffects.BreakCapture jem$break;
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$before(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { jem$break = SandboxCoreEffects.beforeBreak(player, pos); }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$after(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var capture = jem$break; jem$break = null; SandboxCoreEffects.afterBreak(capture, callback.getReturnValue());
    }
}
