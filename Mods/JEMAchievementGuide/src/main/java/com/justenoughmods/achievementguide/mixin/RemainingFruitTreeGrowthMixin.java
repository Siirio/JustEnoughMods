package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.block.FruitTree", remap = false)
public abstract class RemainingFruitTreeGrowthMixin {
    @Unique private final ThreadLocal<NativeFoodSources.Plant> jem$root = new ThreadLocal<>();
    @Inject(method = "growFruitTree", at = @At("HEAD"))
    private void jem$begin(Level level, BlockPos pos, CallbackInfo callback) {
        jem$root.remove();
        if (level instanceof ServerLevel server) {
            var planted = NativePlantProof.original(server, pos, level.getBlockState(pos));
            if (planted != null && planted.kind().equals("cassowary_orchard")) jem$root.set(planted);
        }
    }
    @Inject(method = "growFruitTree", at = @At("RETURN"))
    private void jem$grown(Level level, BlockPos pos, CallbackInfo callback) {
        var planted = jem$root.get(); jem$root.remove();
        if (planted == null || !(level instanceof ServerLevel server)) return;
        var state = server.getBlockState(pos); var upper = server.getBlockState(pos.above());
        if (state.is(upper.getBlock()) && upper.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            NativePlantProof.evolved(server, pos.above(), planted, upper);
        }
    }
}
