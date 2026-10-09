package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.compat.MachineContext;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.saw.SawMovementBehaviour", remap = false)
public abstract class CreateMovingSawMixin {
    @Redirect(method = "onBlockBroken", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter$Tree;destroyBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/BiConsumer;)V"))
    private void jemClaims$tree(TreeCutter.Tree tree, Level level, LivingEntity actor, BiConsumer<BlockPos, ItemStack> drops, MovementContext context, BlockPos target, BlockState state) {
        MachineContext.run(context.contraption.anchor, () -> tree.destroyBlocks(level, actor, drops));
    }

    @Redirect(method = "onBlockBroken", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/AbstractBlockBreakQueue;destroyBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/BiConsumer;)V"))
    private void jemClaims$dynamicTree(AbstractBlockBreakQueue tree, Level level, LivingEntity actor, BiConsumer<BlockPos, ItemStack> drops, MovementContext context, BlockPos target, BlockState state) {
        MachineContext.run(context.contraption.anchor, () -> tree.destroyBlocks(level, actor, drops));
    }
}
