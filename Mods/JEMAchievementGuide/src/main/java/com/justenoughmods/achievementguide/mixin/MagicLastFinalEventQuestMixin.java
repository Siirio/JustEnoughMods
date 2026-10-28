package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastFocusProof;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "net.minecraftforge.eventbus.EventBus", remap = false)
public abstract class MagicLastFinalEventQuestMixin {
    @Inject(method = "post(Lnet/minecraftforge/eventbus/api/Event;)Z", at = @At("RETURN"))
    private void jem$final(Event event, CallbackInfoReturnable<Boolean> callback) { MagicLastFocusProof.finish(event); }
}
