package com.siirio.jemserver.mixin;

import com.siirio.jemserver.ServerConfig;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SleepStatus.class)
public abstract class SleepStatusVoteMixin {
    @Inject(method = "areEnoughDeepSleeping", at = @At("HEAD"), cancellable = true)
    private void jem$voteControlsMorning(int percentage, List<ServerPlayer> players, CallbackInfoReturnable<Boolean> callback) {
        if (ServerConfig.SLEEP_ENABLED.get()) callback.setReturnValue(false);
    }
}
