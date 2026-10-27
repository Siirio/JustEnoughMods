package com.siirio.jemluxium.mixin;

import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia", remap = false)
public abstract class NeoSkyCelestiaOculusGuardMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/ModList;isLoaded(Ljava/lang/String;)Z"
            )
    )
    private static boolean jemLuxium$allowExclusiveOculusInstallation(ModList mods, String modId) {
        return !"oculus".equals(modId) && mods.isLoaded(modId);
    }
}
