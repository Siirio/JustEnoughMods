package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.PrimordialFoodProof;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.PrimordialArmorItem", remap = false)
public abstract class PrimordialBonusQuestMixin {
    @Inject(method = "getExtraSaturationFromArmor", at = @At("RETURN"))
    private static void jem$nativeBonus(LivingEntity actor, CallbackInfoReturnable<Integer> callback) {
        PrimordialFoodProof.nativeBonus(actor, callback.getReturnValueI());
    }
}
