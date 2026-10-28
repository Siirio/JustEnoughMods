package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.crops.TomatoCropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native tomato head/body harvest keeps the living plant and resets its actual age. */
@Mixin(value = TomatoCropBlock.class, remap = false)
public abstract class TomatoGreenhouseHarvestMixin {
    @Shadow protected abstract void dropTomatoes(Level level, BlockPos pos, BlockState state);
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/crops/TomatoCropBlock;dropTomatoes(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void jem$harvest(TomatoCropBlock crop, Level level, BlockPos pos, BlockState before,
                             BlockState original, Level nativeLevel, BlockPos nativePos, Player actor, InteractionHand hand, BlockHitResult hit) {
        boolean covered = actor instanceof ServerPlayer && before.getValue(TomatoCropBlock.AGE) == 4
                && GreenhouseQuestAccess.jem$covered(level, pos);
        dropTomatoes(level, pos, before);
        if (covered && level.getBlockState(pos).equals(before.setValue(TomatoCropBlock.AGE, 1)))
            QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor, "food/greenhouse_harvest");
    }
}
