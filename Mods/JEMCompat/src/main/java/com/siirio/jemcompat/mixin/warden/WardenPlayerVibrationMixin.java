package com.siirio.jemcompat.mixin.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.warden.Warden$VibrationUser")
public abstract class WardenPlayerVibrationMixin {
    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void jemcompat$acceptOnlyPlayerAttributedVibrations(ServerLevel level, BlockPos position,
                                                                GameEvent event, GameEvent.Context context,
                                                                CallbackInfoReturnable<Boolean> callback) {
        if (SculkShriekerPlayerAttributionAccessor.jemcompat$resolvePlayer(context.sourceEntity()) == null) {
            callback.setReturnValue(false);
        }
    }
}
