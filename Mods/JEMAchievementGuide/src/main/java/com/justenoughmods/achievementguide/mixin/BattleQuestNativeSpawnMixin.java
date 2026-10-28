package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.Entity;
@Mixin(net.minecraft.server.level.ServerLevel.class)
public abstract class BattleQuestNativeSpawnMixin {
    @Inject(method="addFreshEntity", at=@At("RETURN"))
    private void jem$spawn(Entity entity, CallbackInfoReturnable<Boolean> ci) { BattleQuestArtifacts.spawned(entity, ci.getReturnValue()); }
}
