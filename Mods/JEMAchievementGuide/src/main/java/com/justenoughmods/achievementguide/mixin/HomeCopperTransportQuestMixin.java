package com.justenoughmods.achievementguide.mixin;

import com.github.smallinger.copperagebackport.entity.ai.behavior.TransportItemsBetweenContainers;
import com.github.smallinger.copperagebackport.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import com.justenoughmods.achievementguide.integration.home.HomeCopperProof;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransportItemsBetweenContainers.class, remap = false)
public abstract class HomeCopperTransportQuestMixin {
    @Shadow private TransportItemTarget target;
    @Inject(method = "pickUpItems", at = @At("HEAD"))
    private void jem$pickupStart(PathfinderMob mob, Container container, CallbackInfo callback) { HomeCopperProof.begin(mob, container, target, true); }
    @Inject(method = "pickUpItems", at = @At("RETURN"))
    private void jem$pickupEnd(PathfinderMob mob, Container container, CallbackInfo callback) { HomeCopperProof.finish(mob, container, true); }
    @Inject(method = "putDownItem", at = @At("HEAD"))
    private void jem$depositStart(PathfinderMob mob, Container container, CallbackInfo callback) { HomeCopperProof.begin(mob, container, target, false); }
    @Inject(method = "putDownItem", at = @At("RETURN"))
    private void jem$depositEnd(PathfinderMob mob, Container container, CallbackInfo callback) { HomeCopperProof.finish(mob, container, false); }
}
