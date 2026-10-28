package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.farm_and_charm.core.entity.PlowCartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Read the native attached puller or its real controlling rider only after actual soil conversion. */
@Mixin(value = PlowCartEntity.class, remap = false)
public abstract class PlowFoodQuestMixin {
    @Redirect(method = "handleServerSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$committedTilling(Level level, BlockPos pos, BlockState result, int flags) {
        BlockState before = level.getBlockState(pos);
        boolean changed = level.setBlock(pos, result, flags);
        if (changed && result.is(Blocks.FARMLAND) && !before.is(Blocks.FARMLAND) && level.getBlockState(pos).is(Blocks.FARMLAND)) {
            Entity driver = ((PlowCartEntity)(Object)this).getDriver();
            ServerPlayer actor = driver instanceof ServerPlayer player ? player
                    : driver != null && driver.getControllingPassenger() instanceof ServerPlayer rider ? rider : null;
            if (actor != null && actor.isAlive() && actor.level() == level && driver.level() == level)
                QuestActionTrigger.INSTANCE.emit(actor, "food/plow_tilled");
        }
        return changed;
    }
}
