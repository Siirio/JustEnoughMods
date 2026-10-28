package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureFallingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public abstract class NatureAnvilLandQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Fallable;onLand(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/item/FallingBlockEntity;)V"))
    private void jem$committedStonePress(Fallable block, Level level, BlockPos pos, BlockState state,
                                         BlockState replaced, FallingBlockEntity falling) {
        NatureFallingProof.landed(block, level, pos, state, replaced, falling);
    }
}
