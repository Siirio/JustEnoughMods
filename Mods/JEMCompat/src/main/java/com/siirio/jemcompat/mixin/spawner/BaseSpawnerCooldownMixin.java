package com.siirio.jemcompat.mixin.spawner;

import com.siirio.jemcompat.feature.spawner.SpawnEggPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerCooldownMixin {
    @Unique private static final String JEM_LAST_SLOW_SPAWN = "JemLastSlowSpawn";
    @Unique private long jem$lastSlowSpawn = Long.MIN_VALUE / 2;

    @Redirect(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$enforcePerSpawnerCooldown(ServerLevel level, Entity entity) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        var rule = id == null ? SpawnEggPolicy.SpawnerRule.YES : SpawnEggPolicy.forEntity(id);
        if (rule == SpawnEggPolicy.SpawnerRule.SLOW_24000_TICKS
                && level.getGameTime() - jem$lastSlowSpawn < 24_000L) return false;
        boolean added = level.tryAddFreshEntityWithPassengers(entity);
        if (added && rule == SpawnEggPolicy.SpawnerRule.SLOW_24000_TICKS) jem$lastSlowSpawn = level.getGameTime();
        return added;
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void jem$loadCooldown(Level level, BlockPos pos, CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(JEM_LAST_SLOW_SPAWN, net.minecraft.nbt.Tag.TAG_LONG))
            jem$lastSlowSpawn = tag.getLong(JEM_LAST_SLOW_SPAWN);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void jem$saveCooldown(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        callback.getReturnValue().putLong(JEM_LAST_SLOW_SPAWN, jem$lastSlowSpawn);
    }
}
