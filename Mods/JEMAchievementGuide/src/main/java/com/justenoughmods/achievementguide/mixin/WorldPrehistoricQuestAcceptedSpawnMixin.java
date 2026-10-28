package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerLevel.class)
public abstract class WorldPrehistoricQuestAcceptedSpawnMixin {
 @Inject(method="addFreshEntity",at=@At("RETURN"))private void jem$accepted(Entity entity,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValueZ()&&!entity.isRemoved()&&entity.level() instanceof ServerLevel level&&level.getEntity(entity.getUUID())==entity)WorldPrehistoricQuestDrops.accepted(entity);}
}
