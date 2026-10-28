package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerLevel.class)
public abstract class PetUtilityQuestSpawnMixin {
 @Inject(method="addFreshEntity",at=@At("RETURN"))private void jem$accepted(Entity e,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValueZ()&&!e.isRemoved()&&e.level() instanceof ServerLevel level&&level.getEntity(e.getUUID())==e){PetUtilityQuestDrops.accepted(e);}}
}
