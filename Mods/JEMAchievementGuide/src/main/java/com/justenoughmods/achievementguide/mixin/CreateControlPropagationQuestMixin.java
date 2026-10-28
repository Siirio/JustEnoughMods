package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateControlProofs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.transmission.GearshiftBlock", remap = false)
public abstract class CreateControlPropagationQuestMixin {
    @Inject(method = {"tick", "m_213897_"}, at = @At("RETURN"), remap = false)
    private void jem$nativeOutputAfterPropagation(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo callback) {
        CreateControlProofs.changed(world, pos, true);
    }
}
