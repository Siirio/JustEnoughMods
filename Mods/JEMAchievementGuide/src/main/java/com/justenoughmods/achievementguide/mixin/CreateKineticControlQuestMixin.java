package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateControlProofs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"com.simibubi.create.content.kinetics.transmission.ClutchBlock", "com.simibubi.create.content.kinetics.transmission.GearshiftBlock"}, remap = false)
public abstract class CreateKineticControlQuestMixin {
    @Inject(method = {"neighborChanged", "m_6861_"}, at = @At("HEAD"), remap = false)
    private void jem$nativeControl(BlockState state, Level world, BlockPos pos, Block changed, BlockPos source, boolean moving, CallbackInfo callback) {
        CreateControlProofs.changing(state, world, pos);
    }
    @Inject(method = {"neighborChanged", "m_6861_"}, at = @At("RETURN"), remap = false)
    private void jem$committedStop(BlockState state, Level world, BlockPos pos, Block changed, BlockPos source, boolean moving, CallbackInfo callback) {
        CreateControlProofs.changed(world, pos, false);
    }
}
