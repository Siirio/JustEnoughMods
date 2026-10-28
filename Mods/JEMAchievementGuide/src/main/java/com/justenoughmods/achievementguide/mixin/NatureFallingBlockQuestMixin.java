package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureFallingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlock.class)
public abstract class NatureFallingBlockQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;fall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/item/FallingBlockEntity;"))
    private FallingBlockEntity jem$nativeAnvilFall(Level level, BlockPos pos, BlockState state) {
        return NatureFallingProof.fall(level, pos, state);
    }
}
