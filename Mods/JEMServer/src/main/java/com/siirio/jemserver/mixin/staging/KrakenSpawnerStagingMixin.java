package com.siirio.jemserver.mixin.staging;

import com.siirio.jemserver.smp.events.StructureStaging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.unusual.block_factorys_bosses.block.entity.KrakenSpawnerBlockEntity", remap = false)
public abstract class KrakenSpawnerStagingMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$waitForStart(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos position, net.minecraft.world.level.block.state.BlockState state, CallbackInfo callback) {
        if (!StructureStaging.allowNativeSpawn(level, position)) callback.cancel();
    }
}
