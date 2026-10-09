package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.NewNetherite_Monstrosity.Netherite_Monstrosity_Entity", remap = false)
public abstract class MonstrosityTerrainMixin {
    @Redirect(method = {"berserkBlockBreaking", "BlockBreaking"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState jemclaims$terrainCandidate(Level level, BlockPos position) {
        return BossTerrainProtection.allows(level, position) ? level.getBlockState(position) : Blocks.AIR.defaultBlockState();
    }

    @Redirect(method = "doAbsorptionEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jemclaims$absorption(Level level, BlockPos position, BlockState state) {
        return BossTerrainProtection.allows(level, position) && level.setBlockAndUpdate(position, state);
    }
}
