package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFishingConversionProof;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "koala.fishingreal.FishingRealForge", remap = false)
public abstract class RemainingFishingConversionEventMixin {
    @Inject(method = "onItemFished", at = @At("HEAD"))
    private static void jem$begin(ItemFishedEvent event, CallbackInfo callback) { NativeFishingConversionProof.begin(event); }
    @Inject(method = "onItemFished", at = @At("RETURN"))
    private static void jem$end(ItemFishedEvent event, CallbackInfo callback) { NativeFishingConversionProof.end(); }
}
