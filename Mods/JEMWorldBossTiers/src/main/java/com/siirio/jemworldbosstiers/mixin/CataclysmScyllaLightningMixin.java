package com.siirio.jemworldbosstiers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Scylla.Scylla_Entity", remap = false)
public abstract class CataclysmScyllaLightningMixin {
    @Inject(method = "spawnLightning(DDDDFIF)V", at = @At("HEAD"), cancellable = true,
            require = 1, expect = 1, remap = false)
    private void jem$keepLightningInsideLoadedTerrain(double x, double z, double minY, double maxY,
            float rotation, int delay, float scale, CallbackInfo callback) {
        Level level = ((Entity) (Object) this).level();
        if (!level.hasChunkAt(BlockPos.containing(x, level.getMinBuildHeight(), z))) {
            callback.cancel();
        }
    }
}
