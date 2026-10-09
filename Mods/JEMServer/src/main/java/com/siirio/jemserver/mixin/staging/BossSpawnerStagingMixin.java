package com.siirio.jemserver.mixin.staging;

import com.siirio.jemserver.smp.events.StructureStaging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.unusual.block_factorys_bosses.block.entity.BossSpawnerBlockEntity", remap = false)
public abstract class BossSpawnerStagingMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$waitForStart(net.minecraft.world.level.Level level, CallbackInfo callback) {
        if (!StructureStaging.allowNative(level, ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getBlockPos())) callback.cancel();
    }
}
