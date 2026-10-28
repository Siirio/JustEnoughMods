package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.block.FruitTree", remap = false)
public abstract class RemainingPrimalFruitHarvestMixin {
    @Unique private static final ThreadLocal<NativeHarvestProof.Run> jem$harvest = new ThreadLocal<>();
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$begin(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        jem$harvest.remove(); var actor = RemainingFoodActors.real(player);
        if (actor != null) jem$harvest.set(NativeHarvestProof.begin(actor, pos, state, NativeHarvestProof.plantedKind(actor, pos, state)));
    }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$end(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$harvest.get(); jem$harvest.remove();
        NativeHarvestProof.commit(run, callback.getReturnValue().consumesAction() && NativeHarvestProof.reset(state, level.getBlockState(pos)));
    }
}
