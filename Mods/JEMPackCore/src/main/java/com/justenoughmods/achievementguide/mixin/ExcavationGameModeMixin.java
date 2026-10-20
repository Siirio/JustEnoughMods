package com.justenoughmods.achievementguide.mixin;

import com.siirio.jempackcore.enchantment.ExcavationMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public abstract class ExcavationGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Shadow(remap = false)
    private boolean removeBlock(BlockPos pos, boolean canHarvest) { throw new AssertionError(); }

    @Redirect(method = {"tick", "destroyAndAck"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean excavationMining(ServerPlayerGameMode gameMode, BlockPos pos) {
        return ExcavationMining.destroyBlock(player, pos);
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            remap = false))
    private boolean excavationRemoval(ServerPlayerGameMode gameMode, BlockPos pos, boolean canHarvest) {
        boolean removed = removeBlock(pos, canHarvest);
        ExcavationMining.blockRemoved(player, pos, removed);
        return removed;
    }
}
