package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.OfficialMarkerTravel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.mods.gui.Waypoint;

/** Preserve Xaero's own menu row, label and ordering; authorize only official identities. */
@Pseudo
@Mixin(targets = "xaero.map.mods.gui.WaypointReader$4", remap = false)
public abstract class OfficialWaypointOptionMixin {
    @Shadow @Final private Waypoint val$element;

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void jem$officialAction(CallbackInfoReturnable<Boolean> callback) {
        if (OfficialMarkerTravel.available(val$element.getOriginal())) callback.setReturnValue(true);
    }
}
