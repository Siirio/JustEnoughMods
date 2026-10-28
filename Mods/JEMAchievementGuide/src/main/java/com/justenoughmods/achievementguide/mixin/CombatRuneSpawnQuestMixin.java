package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class CombatRuneSpawnQuestMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$spawn(Entity entity, CallbackInfoReturnable<Boolean> callback) { CombatRuneProof.spawned(entity, callback.getReturnValue()); com.justenoughmods.achievementguide.integration.combattools.CombatRegrowthProof.spawned(entity, callback.getReturnValue()); }
}
