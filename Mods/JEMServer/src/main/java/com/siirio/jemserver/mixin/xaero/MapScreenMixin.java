package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.client.xaero.MapClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep shared-server snapshot refresh; Settlements owns its optional map extension. */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class MapScreenMixin {
    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void jem$request(CallbackInfo callback) { if (MapClient.connected()) MapNetwork.request(); }
}
