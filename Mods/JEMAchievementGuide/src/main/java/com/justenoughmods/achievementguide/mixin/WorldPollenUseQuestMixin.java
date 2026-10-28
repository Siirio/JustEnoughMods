package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldBambooProof;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.chybx.spideroverhaul.Spideroverhaul", remap = false)
public abstract class WorldPollenUseQuestMixin {
    @Inject(method = "onRightClickBlock", at = @At("HEAD"))
    private void jem$beginNativePollen(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) { WorldBambooProof.begin(event); }
    @Inject(method = "onRightClickBlock", at = @At("RETURN"))
    private void jem$paidNativePollen(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) { WorldBambooProof.end(event); }
}
