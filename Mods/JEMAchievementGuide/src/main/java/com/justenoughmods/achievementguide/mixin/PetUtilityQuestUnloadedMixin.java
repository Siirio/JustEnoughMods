package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
public abstract class PetUtilityQuestUnloadedMixin {
 @Inject(method="tick",at=@At("RETURN"))private void jem$safe(CallbackInfo ci){var e=(Mob)(Object)this;if(!e.level().isClientSide && PetUtilityQuestProof.data(e).hasUUID("unload"))PetUtilityQuestWagon.unloaded(e);}
}
