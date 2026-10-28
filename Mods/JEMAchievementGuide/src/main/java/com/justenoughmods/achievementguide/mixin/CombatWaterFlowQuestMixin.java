package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatWaterProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FlowingFluid.class)
public abstract class CombatWaterFlowQuestMixin {
    @Inject(method = "spreadTo", at = @At("RETURN"))
    private void jem$flow(LevelAccessor level, BlockPos pos, BlockState before, Direction direction, FluidState fluid, CallbackInfo callback) { if (!before.equals(level.getBlockState(pos))) CombatWaterProof.flowed(level, pos, direction); }
}
