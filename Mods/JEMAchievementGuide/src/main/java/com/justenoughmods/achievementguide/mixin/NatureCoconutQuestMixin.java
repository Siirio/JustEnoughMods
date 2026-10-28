package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureActors;
import com.justenoughmods.achievementguide.integration.nature.NatureDropProof;
import com.justenoughmods.achievementguide.integration.nature.NatureFallingProof;
import net.hibiscus.naturespirit.blocks.CoconutBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CoconutBlock.class, remap = false)
public abstract class NatureCoconutQuestMixin {
    @Redirect(method = "m_5581_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", remap = false))
    private boolean jem$nativeDetachedCoconut(Level world, BlockPos pos, BlockState replacement,
                                             Level ownerWorld, BlockState original, BlockHitResult hit, Projectile projectile) {
        BlockState before = world.getBlockState(pos);
        boolean accepted = world.setBlockAndUpdate(pos, replacement);
        if (accepted && world == ownerWorld && pos.equals(hit.getBlockPos()) && before.equals(original)
                && original.getValue(CoconutBlock.FACING) != Direction.UP && replacement.getValue(CoconutBlock.FACING) == Direction.UP
                && world.getBlockState(pos).equals(replacement) && projectile.getOwner() instanceof ServerPlayer actor
                && NatureActors.eligible(actor) && actor.level() == world) {
            NatureFallingProof.attributed(actor, pos, replacement, "Coconut");
        }
        return accepted;
    }

    @Redirect(method = "m_5581_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z", remap = false))
    private boolean jem$nativeCoconutLoot(Level world, BlockPos pos, boolean drops,
                                        Level ownerWorld, BlockState original, BlockHitResult hit, Projectile projectile) {
        if (!(projectile.getOwner() instanceof ServerPlayer actor) || !NatureActors.eligible(actor)
                || actor.level() != world || world != ownerWorld || !pos.equals(hit.getBlockPos()) || !world.getBlockState(pos).equals(original)) {
            return world.destroyBlock(pos, drops);
        }
        var run = new NatureDropProof.Run(actor, pos, "natures_spirit:coconut");
        boolean accepted = NatureDropProof.nativeCall(run, () -> world.destroyBlock(pos, drops));
        if (accepted && run.accepted && !world.getBlockState(pos).equals(original)) NatureActors.emit(actor, "v3_world_root_and_crown");
        return accepted;
    }

    @Redirect(method = "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;m_201971_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/item/FallingBlockEntity;", remap = false))
    private FallingBlockEntity jem$nativeFallingCoconut(Level world, BlockPos pos, BlockState state) {
        return NatureFallingProof.fall(world, pos, state);
    }
}
