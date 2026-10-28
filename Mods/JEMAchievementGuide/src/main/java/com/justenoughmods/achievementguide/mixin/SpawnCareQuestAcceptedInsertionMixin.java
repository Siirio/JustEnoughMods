package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.SpawnCareQuestRun;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class SpawnCareQuestAcceptedInsertionMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$inserted(Entity entity, CallbackInfoReturnable<Boolean> callback) { SpawnCareQuestRun.inserted(entity, callback.getReturnValue()); com.justenoughmods.achievementguide.integration.spawncare.SpawnCareQuestNaturalRun.inserted(entity, callback.getReturnValue()); }
}
