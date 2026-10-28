package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.ArrowQuestActor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.morearrows.specialarrowentities.headless.ExtinguishingArrowEntity", remap = false)
public abstract class ExtinguishingArrowQuestMixin {
    @Redirect(method = "applyExtinguishingEffectToSurrounding", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_7471_(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jem$actualFireExtinguished(Level level, BlockPos position, boolean moved) {
        boolean burning = level.getBlockState(position).is(BlockTags.FIRE);
        boolean removed = level.removeBlock(position, moved);
        var actor = ArrowQuestActor.get((Projectile) (Object) this);
        if (removed && burning && actor != null && !level.getBlockState(position).is(BlockTags.FIRE)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_arrow_tools");
        }
        return removed;
    }

    @Redirect(method = "applyExtinguishingEffectToSurrounding", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 2)
    private boolean jem$actualLavaConverted(Level level, BlockPos position, BlockState state) {
        boolean lava = level.getFluidState(position).is(FluidTags.LAVA);
        boolean changed = level.setBlockAndUpdate(position, state);
        var actor = ArrowQuestActor.get((Projectile) (Object) this);
        if (changed && lava && actor != null && level.getBlockState(position).equals(state)
                && !level.getFluidState(position).is(FluidTags.LAVA)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_arrow_tools");
        }
        return changed;
    }
}
