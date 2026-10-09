package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Death_Laser_Beam_Entity", remap = false)
public abstract class BossDeathLaserTerrainMixin {
    @Shadow public LivingEntity caster;

    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jemclaims$fire(Level level, BlockPos position, BlockState state) {
        return (!BossTerrainProtection.isBoss(caster) || BossTerrainProtection.allows(level, position))
                && level.setBlockAndUpdate(position, state);
    }
}
