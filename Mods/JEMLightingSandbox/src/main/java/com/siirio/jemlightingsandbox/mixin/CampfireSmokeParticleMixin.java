package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.SandboxWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CampfireSmokeParticle.class)
abstract class CampfireSmokeParticleMixin extends TextureSheetParticle {
    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @ModifyArgs(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/CampfireSmokeParticle;move(DDD)V"))
    private void jem$driftSmoke(Args args) {
        var wind = SandboxWeather.campfireWind(level, getBoundingBox().getCenter());
        if (wind != Vec3.ZERO) {
            args.set(0, ((Double) args.get(0)) + wind.x);
            args.set(2, ((Double) args.get(2)) + wind.z);
        }
    }
}
