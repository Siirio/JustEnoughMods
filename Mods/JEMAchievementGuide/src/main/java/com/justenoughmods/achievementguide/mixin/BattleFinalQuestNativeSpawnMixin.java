package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=net.minecraft.server.level.ServerLevel.class)
public abstract class BattleFinalQuestNativeSpawnMixin {
    @Inject(method="addFreshEntity",at=@At("RETURN"))private void jem$spawn(net.minecraft.world.entity.Entity output,CallbackInfoReturnable<Boolean> ci){BattleFinalQuestPassive.spawned(output,ci.getReturnValue());}
}
