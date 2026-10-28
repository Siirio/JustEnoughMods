package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tiestoettoet.create_train_parts.content.decoration.slidingWindow.SlidingWindowBlock", remap = false)
public abstract class TrainWindowQuestMixin {
    @Inject(method = "toggle(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Ljava/lang/Boolean;I)V", at = @At("RETURN"), remap = false)
    private void jem$actualAccessTransition(BlockState before, Level world, BlockPos pos, Player actor, Boolean requested, int flags, CallbackInfo callback) {
        BlockState after = world.getBlockState(pos);
        if (actor instanceof ServerPlayer player && before.getBlock() == after.getBlock()
                && before.hasProperty(BlockStateProperties.OPEN) && after.hasProperty(BlockStateProperties.OPEN)
                && before.getValue(BlockStateProperties.OPEN) != after.getValue(BlockStateProperties.OPEN))
            QuestActionTrigger.INSTANCE.emit(player, "create_train_access_transition");
    }
}
