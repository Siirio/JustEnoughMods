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
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Leviathan.Dimensional_Rift_Entity", remap = false)
public abstract class BossRiftTerrainMixin {
    @Shadow public abstract LivingEntity getOwner();

    @Redirect(method = "berserkBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jemclaims$rift(Level level, BlockPos position, BlockState state, int flags) {
        return (!BossTerrainProtection.isBoss(getOwner()) || BossTerrainProtection.allows(level, position))
                && level.setBlock(position, state, flags);
    }
}
