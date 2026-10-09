package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Lava_Bomb_Entity", remap = false)
public abstract class BossLavaBombTerrainMixin {
    @Redirect(method = {"doTerrainEffects", "m_142687_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jemclaims$lava(Level level, BlockPos position, BlockState state) {
        return (!BossTerrainProtection.isBoss((Entity) (Object) this) || BossTerrainProtection.allows(level, position))
                && level.setBlockAndUpdate(position, state);
    }
}
