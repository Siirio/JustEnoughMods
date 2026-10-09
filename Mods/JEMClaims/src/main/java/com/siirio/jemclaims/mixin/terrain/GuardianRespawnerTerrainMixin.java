package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ender_Guardian_Entity", remap = false)
public abstract class GuardianRespawnerTerrainMixin {
    @Redirect(method = "Respawner", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jemclaims$placement(ServerLevel level, BlockPos position, BlockState state, int flags) {
        return BossTerrainProtection.allows(level, position) && level.setBlock(position, state, flags);
    }
}
