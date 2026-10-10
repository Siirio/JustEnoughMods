package com.siirio.jemdrill.mixin;

import com.siirio.jemdrill.drill.MovingDrillService;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class AbstractContraptionEntityMixin {
    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void jemcompat$serviceDrills(Player player, BlockPos localPos, Direction side, InteractionHand hand, CallbackInfoReturnable<Boolean> callback) {
        if (MovingDrillService.service((AbstractContraptionEntity) (Object) this, player, localPos, hand)) {
            callback.setReturnValue(true);
        }
    }
}
