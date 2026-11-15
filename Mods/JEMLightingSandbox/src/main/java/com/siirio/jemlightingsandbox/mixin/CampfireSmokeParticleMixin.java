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
    @Unique private static final double JEM_MAX_PLUME_LENGTH = 45.0;
    @Unique private boolean jem$plume;
    @Unique private double jem$plumeCeiling;
    @Unique private double jem$sourceHeight;
    @Unique private float jem$baseQuadSize;
    @Unique private double jem$riseScale;
    @Unique private float jem$plumeOpacity;
    @Unique private int jem$fadeTicks;
    @Unique private double jem$distanceTraveled;

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
        int plumeLifetime = lifetime + (signal ? 100 : 140);
        int fadeTicks = (signal ? 160 : 130) + neighbors * 4;
        int riseTicks = plumeLifetime - fadeTicks / 4;
        // Vanilla subtracts gravity once before every move. Integrate that exact sequence.
        double nativeRise = yd * riseTicks - gravity * riseTicks * (riseTicks + 1) / 2.0;
        if (nativeRise <= 0.0) {
            return;
        }

        float density = neighbors / 7f;
        if (LightingSettings.darkCampfireSmoke()) {
            float shade = 0.70f - 0.32f * density;
            setColor(rCol * shade, gCol * shade, bCol * shade);
        }
        quadSize *= 1f + 0.45f * density;
        jem$baseQuadSize = quadSize;

        double baseHeight = signal ? 33.0 : 22.0;
        jem$sourceHeight = source.getY();
        jem$plumeCeiling = jem$sourceHeight + Math.min(JEM_MAX_PLUME_LENGTH, baseHeight + 1.5 * neighbors);
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
        if (!jem$plume) {
            smoke.move(x + wind.x, y, z + wind.z);
            return;
        }
        y = Math.min(y * jem$riseScale, Math.max(0.0, jem$plumeCeiling - this.y));
        double bendStart = jem$sourceHeight + 5.0;
        double bendHeight = jem$plumeCeiling - bendStart;
        double start = Math.max(0.0, (this.y - bendStart) / bendHeight);
        double end = Math.max(0.0, (this.y + y - bendStart) / bendHeight);
        var bend = wind.scale(500.0 * (end * end - start * start));
        x += wind.x + bend.x;
        z += wind.z + bend.z;

        double remaining = Math.max(0.0, JEM_MAX_PLUME_LENGTH - jem$distanceTraveled);
        double length = Math.sqrt(x * x + y * y + z * z);
        double scale = length > remaining ? remaining / length : 1.0;
        double previousX = this.x;
        double previousY = this.y;
        double previousZ = this.z;
        smoke.move(x * scale, y * scale, z * scale);
        double dx = this.x - previousX;
        double dy = this.y - previousY;
        double dz = this.z - previousZ;
        jem$distanceTraveled += Math.sqrt(dx * dx + dy * dy + dz * dz);
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
        quadSize = jem$baseQuadSize * (1F + 0.15F * (float) Math.max(0.0, y - jem$sourceHeight - 5.0));
        float fade = Math.max(0f, (age - lifetime + jem$fadeTicks) / (float) jem$fadeTicks);
        float tipFade = (float) Math.max(0.0, (jem$distanceTraveled - 39.0) / (JEM_MAX_PLUME_LENGTH - 39.0));
        fade = Math.min(1f, Math.max(fade, tipFade));
        alpha = jem$plumeOpacity * (1f - fade * fade * (3f - 2f * fade));
    }
}
