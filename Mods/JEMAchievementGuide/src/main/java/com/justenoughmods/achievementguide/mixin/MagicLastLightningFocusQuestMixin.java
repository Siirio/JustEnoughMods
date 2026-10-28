package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastFocusProof;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.player.ServerPlayerEvents", remap = false)
public abstract class MagicLastLightningFocusQuestMixin {
    @Inject(method = "useOnEntityEvent", at = @At("HEAD"))
    private static void jem$before(PlayerInteractEvent.EntityInteractSpecific event, CallbackInfo callback) { MagicLastFocusProof.begin(event); }
    @Inject(method = "useOnEntityEvent", at = @At("RETURN"))
    private static void jem$after(PlayerInteractEvent.EntityInteractSpecific event, CallbackInfo callback) { MagicLastFocusProof.end(); }
}
