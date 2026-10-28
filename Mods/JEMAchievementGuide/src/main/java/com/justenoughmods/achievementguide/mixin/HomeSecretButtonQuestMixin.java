package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSignalProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.larsmans.infinitybuttons.block.custom.secretbutton.AbstractSecretButton", remap = false)
public abstract class HomeSecretButtonQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$before(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeSignalProof.begin(level, pos, player, hand);
    }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$after(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                           BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        HomeSignalProof.finish(level, pos, player, callback.getReturnValue(), false);
    }
}
