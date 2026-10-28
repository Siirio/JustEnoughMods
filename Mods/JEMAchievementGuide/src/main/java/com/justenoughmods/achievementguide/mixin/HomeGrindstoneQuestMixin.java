package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.siirio.jemcompat.feature.enchanting.GrindstoneExtraction", remap = false)
public abstract class HomeGrindstoneQuestMixin {
    // This exact owner call exists only after accepted book spawn, first-enchantment removal,
    // canonical receipt settlement and XP transfer. Earlier returns never reach this seam.
    @Inject(method = "use", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;setCancellationResult(Lnet/minecraft/world/InteractionResult;)V",
            shift = At.Shift.AFTER, remap = false))
    private static void jem$settled(PlayerInteractEvent.RightClickBlock event, CallbackInfo callback) {
        HomeInteractionProof.emit(event.getEntity(), "v3_home_enchantment_extract");
    }
}
