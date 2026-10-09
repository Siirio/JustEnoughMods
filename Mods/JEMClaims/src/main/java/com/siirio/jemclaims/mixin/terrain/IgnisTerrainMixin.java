package com.siirio.jemclaims.mixin.terrain;

import com.siirio.jemclaims.compat.terrain.BossTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity", remap = false)
public abstract class IgnisTerrainMixin {
    @Redirect(method = {"ShieldSmashDamage", "UltimateAttack"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46953_(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean jemclaims$terrain(Level level, BlockPos position, boolean drops, Entity entity) {
        return BossTerrainProtection.allows(level, position) && level.destroyBlock(position, drops, entity);
    }
}
