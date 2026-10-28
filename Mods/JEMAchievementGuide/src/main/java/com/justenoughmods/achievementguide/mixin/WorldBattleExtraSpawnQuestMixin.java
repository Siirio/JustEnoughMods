package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class WorldBattleExtraSpawnQuestMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$realNativeSpawn(Entity entity, CallbackInfoReturnable<Boolean> ci) { WorldBattleExtraProof.spawned(entity, ci.getReturnValue()); }
}
