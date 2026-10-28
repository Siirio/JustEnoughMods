package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateControlProofs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeverBlock.class)
public abstract class CreateLeverControlQuestMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeverBlock;pull(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState jem$actualPlayerControl(LeverBlock lever, BlockState state, Level world, BlockPos pos,
                                               BlockState original, Level originalWorld, BlockPos originalPos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
        return player instanceof ServerPlayer actor ? CreateControlProofs.command(actor, () -> lever.pull(state, world, pos))
                : lever.pull(state, world, pos);
    }
}
