package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateControlProofs;
import com.justenoughmods.achievementguide.integration.create.CreateRailCouplerRun;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.railwayteam.railways.content.coupling.coupler.TrackCouplerBlock", remap = false)
public abstract class RailCouplerControlQuestMixin {
    @Inject(method = {"neighborChanged", "m_6861_"}, at = @At("RETURN"), remap = false)
    private void jem$actualEdge(BlockState before, Level world, BlockPos pos, Block source, BlockPos sourcePos, boolean moving, CallbackInfo callback) {
        BlockState after = world.getBlockState(pos);
        if (before.getBlock() == after.getBlock() && before.hasProperty(BlockStateProperties.POWERED)
                && after.hasProperty(BlockStateProperties.POWERED) && !before.getValue(BlockStateProperties.POWERED)
                && after.getValue(BlockStateProperties.POWERED) && world.getBlockEntity(pos) instanceof CreateRailCouplerRun coupler)
            coupler.jem$nativeEdge(CreateControlProofs.currentActor());
    }
}
