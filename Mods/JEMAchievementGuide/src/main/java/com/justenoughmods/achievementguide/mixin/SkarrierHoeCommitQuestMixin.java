package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class SkarrierHoeCommitQuestMixin {
    @Inject(method = "useOn", at = @At("HEAD"))
    private void jem$begin(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) { SkarrierWorkshopProof.workBegin(context.getPlayer(), context.getClickedPos()); }
    @Inject(method = "useOn", at = @At("RETURN"))
    private void jem$end(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) { SkarrierWorkshopProof.workEnd(callback.getReturnValue().consumesAction()); }
}
