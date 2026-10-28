package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestPhoenix;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityType.class)
public abstract class BattleQuestPhoenixSpawnMixin {
    @Inject(method="spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;)Lnet/minecraft/world/entity/Entity;",at=@At("RETURN"))
    private void jem$spawn(ServerLevel level,BlockPos position,MobSpawnType reason,CallbackInfoReturnable<Entity> ci){BattleQuestPhoenix.spawned(ci.getReturnValue());}
}
