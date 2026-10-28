package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateBearingRun;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
@Mixin(targets = {"com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock",
        "com.simibubi.create.content.contraptions.bearing.WindmillBearingBlock"}, remap = false)
public abstract class CreateBearingCommandQuestMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("RETURN"), remap = false)
    private void jem$nativeAssemblyCommand(BlockState state, Level world, BlockPos position, Player player,
                                           InteractionHand hand, BlockHitResult hit,
                                           CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof ServerPlayer actor && callback.getReturnValue() == InteractionResult.SUCCESS
                && world.getBlockEntity(position) instanceof CreateBearingRun bearing) {
            bearing.jem$assemblyCommand(actor);
        }
    }
}
