package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeCopperProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.smallinger.copperagebackport.event.CopperGolemSpawnLogic", remap = false)
public abstract class HomeCopperSpawnQuestMixin {
    @Redirect(method = "trySpawnCopperGolem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z", remap = false))
    private static boolean jem$nativeSpawn(ServerLevel level, Entity entity, ServerLevel owner, BlockPos pumpkin, Direction direction) {
        boolean accepted = level.addFreshEntity(entity);
        if (accepted) HomeCopperProof.created(entity, pumpkin);
        return accepted;
    }
}
