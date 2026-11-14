package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.CampfirePlumes;
import com.siirio.jemlightingsandbox.LightingSettings;
import com.siirio.jemlightingsandbox.SandboxWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireSmokeParticle.class)
abstract class CampfireSmokeParticleMixin extends TextureSheetParticle {
    @Unique private boolean jem$plume;
    @Unique private double jem$plumeCeiling;
    @Unique private double jem$riseScale;
    @Unique private float jem$plumeOpacity;
    @Unique private int jem$fadeTicks;

    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDZ)V", at = @At("RETURN"))
    private void jem$configurePlume(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, boolean signal, CallbackInfo ci) {
        if (!LightingSettings.atmosphereEnabled()) {
            return;
        }
        BlockPos source = CampfirePlumes.source(level, x, y, z);
        if (source == null) {
            return;
        }

        int neighbors = CampfirePlumes.neighbors(level, source);
        int plumeLifetime = lifetime + (signal ? 30 : 80);
        int fadeTicks = (signal ? 140 : 100) + neighbors * 4;
        int riseTicks = plumeLifetime - fadeTicks / 4;
        // Vanilla subtracts gravity once before every move. Integrate that exact sequence.
        double nativeRise = yd * riseTicks - gravity * riseTicks * (riseTicks + 1) / 2.0;
        if (nativeRise <= 0.0) {
            return;
        }

        float density = neighbors / 7f;
        float shade = 1f - 0.64f * density;
        setColor(shade, shade, shade);
        quadSize *= 1f + 0.45f * density;

        double baseHeight = signal ? 18.5 : 10.0;
        jem$plumeCeiling = source.getY() + Math.min(20.0, baseHeight + 1.5 * neighbors);
        lifetime = plumeLifetime;
        jem$fadeTicks = fadeTicks;
        jem$riseScale = (jem$plumeCeiling - y) / nativeRise;
        jem$plumeOpacity = 0.1f * density;
        jem$plume = true;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/CampfireSmokeParticle;move(DDD)V"))
    private void jem$driftSmoke(CampfireSmokeParticle smoke, double x, double y, double z) {
        var wind = SandboxWeather.campfireWind(level, getBoundingBox().getCenter());
        if (jem$plume) {
            y = Math.min(y * jem$riseScale, Math.max(0.0, jem$plumeCeiling - this.y));
        }
        smoke.move(x + wind.x, y, z + wind.z);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jem$fadePlume(CallbackInfo ci) {
        if (!jem$plume) {
            return;
        }
        if (age == 1) {
            // The native cosy and signal providers set their initial alpha after construction.
            jem$plumeOpacity = Math.min(1f, alpha + jem$plumeOpacity);
        }
        float fade = Math.max(0f, (age - lifetime + jem$fadeTicks) / (float) jem$fadeTicks);
        alpha = jem$plumeOpacity * (1f - fade * fade * (3f - 2f * fade));
    }
}
