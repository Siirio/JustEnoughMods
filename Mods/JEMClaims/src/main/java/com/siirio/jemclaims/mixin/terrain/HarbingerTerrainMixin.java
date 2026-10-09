package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Harbinger_Entity", remap = false)
public abstract class HarbingerTerrainMixin {
    @Redirect(method = "destoryblock2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canEntityDestroy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jemclaims$terrain(BlockState state, BlockGetter level, BlockPos position, Entity entity) {
        return BossTerrainProtection.allows(level, position) && state.canEntityDestroy(level, position, entity);
    }
}
