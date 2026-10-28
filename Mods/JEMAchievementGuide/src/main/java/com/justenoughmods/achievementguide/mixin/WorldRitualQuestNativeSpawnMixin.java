package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.server.level.ServerLevel.class)
public abstract class WorldRitualQuestNativeSpawnMixin {
    @Inject(method="addFreshEntity",at=@At("RETURN"))private void jem$spawn(net.minecraft.world.entity.Entity entity,CallbackInfoReturnable<Boolean> ci){WorldRitualQuestEctoplasm.spawned(entity,ci.getReturnValue());WorldRitualQuestRitual.spawned(entity,ci.getReturnValue());}
}
