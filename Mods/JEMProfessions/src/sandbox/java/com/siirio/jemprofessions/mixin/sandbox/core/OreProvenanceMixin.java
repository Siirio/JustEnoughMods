package com.siirio.jemprofessions.mixin.sandbox.core;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runtime ore assignment is placement, unlike WorldGenRegion generation and chunk deserialization. */
@Mixin(Level.class)
public abstract class OreProvenanceMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void jem$orePlacement(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerLevel level && level.getServer().isSingleplayer() && state.is(Tags.Blocks.ORES)
                && level.hasChunkAt(pos) && !level.getBlockState(pos).equals(state)) SandboxCoreEffects.placedOre(level, pos);
    }
}
