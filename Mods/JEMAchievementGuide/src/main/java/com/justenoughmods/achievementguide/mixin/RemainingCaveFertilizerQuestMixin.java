package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFertilizerProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.FertilizerItem", remap = false)
public abstract class RemainingCaveFertilizerQuestMixin {
    @Inject(method = "m_6225_", at = @At("HEAD"))
    private void jem$begin(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) { NativeFertilizerProof.begin(RemainingFoodActors.real(context.getPlayer()), context.getItemInHand()); }
    @Inject(method = "m_6225_", at = @At("RETURN"))
    private void jem$end(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) { NativeFertilizerProof.end(callback.getReturnValue().consumesAction()); }
}
