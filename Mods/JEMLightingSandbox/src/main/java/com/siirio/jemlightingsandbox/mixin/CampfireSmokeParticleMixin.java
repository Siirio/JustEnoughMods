package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.SandboxWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireSmokeParticle.class)
abstract class CampfireSmokeParticleMixin extends TextureSheetParticle {
    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/CampfireSmokeParticle;move(DDD)V"))
    private void jem$driftSmoke(CampfireSmokeParticle smoke, double x, double y, double z) {
        var wind = SandboxWeather.campfireWind(level, getBoundingBox().getCenter());
        smoke.move(x + wind.x, y, z + wind.z);
    }
}
