package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class CaveQuestDropSpawnMixin {
    @Inject(method="addFreshEntity",at=@At("RETURN"))private void jem$nativeAcceptedDrop(Entity entity,CallbackInfoReturnable<Boolean> ci){CaveQuestDrops.spawned(entity,ci.getReturnValue());com.justenoughmods.achievementguide.integration.caves.CaveQuestDetonation.spawned(entity,ci.getReturnValue());}
}
