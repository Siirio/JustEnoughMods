package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureChunkOrigins;
import com.justenoughmods.achievementguide.integration.nature.NatureNativeFacts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Injection runs after the upstream override has merged during the MAIN phase. */
@Mixin(value = CoralBlock.class, priority = 1100)
public abstract class NatureCalciteGrowthQuestMixin {
    @Redirect(method = "m_213898_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = false))
    private boolean jem$committedMaturity(ServerLevel world, BlockPos growth, BlockState replacement, int flags,
                                          BlockState coralState, ServerLevel ownerWorld, BlockPos coral, RandomSource random) {
        boolean mature = NatureNativeFacts.block(world.getBlockState(growth), "large_calcite_bud")
                && NatureNativeFacts.block(replacement, "calcite_cluster");
        boolean accepted = world.setBlock(growth, replacement, flags);
        if (accepted && mature && world == ownerWorld && world.getBlockState(growth).equals(replacement)) {
            CompoundTag origin = new CompoundTag();
            origin.putLong("coral", coral.asLong());
            NatureChunkOrigins.put(world, growth, "Calcite", origin);
        }
        return accepted;
    }
}
