package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setBlocksDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"))
    private void jemLightingSandbox$invalidateTerrainLighting(BlockPos position, BlockState previous,
                                                              BlockState current, CallbackInfo callback) {
        TerrainLighting.invalidate(position);
    }
}
