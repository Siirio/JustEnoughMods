package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import com.justenoughmods.achievementguide.integration.food.NativeCampfireGameProof;
import com.justenoughmods.achievementguide.integration.food.TideCatchProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class RemainingFoodNativeInsertionMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$accepted(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        NativeHarvestProof.inserted(entity, callback.getReturnValue());
        NativeCampfireGameProof.inserted(entity, callback.getReturnValue());
        // Observe the actual native result after any fishing owner's XP or spawn policy.
        if (ModList.get().isLoaded("tide")) TideCatchProof.item(entity, callback.getReturnValue());
    }
}
