package com.siirio.jemserver.mixin.fishing;

import com.li64.tide.data.informational.FishingInfoManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FishingInfoManager.class, remap = false)
public abstract class TideInformationalBlockScanMixin {
    @Redirect(method = "scanForNearbyInformationalBlocks(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Set;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = true))
    private static BlockState jemLoadedInformationalBlockState(ServerLevel level, BlockPos pos) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return level.getBlockState(pos);
    }
}
