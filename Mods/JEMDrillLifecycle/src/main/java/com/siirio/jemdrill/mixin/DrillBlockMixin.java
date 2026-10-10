package com.siirio.jemdrill.mixin;

import com.siirio.jemdrill.drill.StationaryDrillService;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DrillBlock.class, remap = false)
public abstract class DrillBlockMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$serviceDrills(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = StationaryDrillService.service(level, pos, player, hand);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}
