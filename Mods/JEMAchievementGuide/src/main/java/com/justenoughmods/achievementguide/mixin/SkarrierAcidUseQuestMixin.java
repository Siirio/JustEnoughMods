package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.item.AcidicBileItem", remap = false)
public abstract class SkarrierAcidUseQuestMixin {
    @Inject(method = "m_6225_", at = @At("HEAD"))
    private void jem$begin(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        SkarrierWorkshopProof.beginBlock("acid", context.getPlayer(), null, context.getItemInHand(), context.getClickedPos());
    }
    @Inject(method = "m_6225_", at = @At("RETURN"))
    private void jem$end(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) { SkarrierWorkshopProof.end(); }
}
