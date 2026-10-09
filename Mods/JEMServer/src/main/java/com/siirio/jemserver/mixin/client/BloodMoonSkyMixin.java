package com.siirio.jemserver.mixin.client;

import com.siirio.jemserver.client.smp.EventBoundaryRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelTimeAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class BloodMoonSkyMixin implements LevelTimeAccess {
    private static final float MIDNIGHT_ANGLE = .5f;

    @Override
    public float getTimeOfDay(float partialTick) {
        var level = (ClientLevel) (Object) this;
        return EventBoundaryRenderer.bloodMoon() ? MIDNIGHT_ANGLE : level.dimensionType().timeOfDay(level.dayTime());
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void jem$nightSky(Vec3 position, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (EventBoundaryRenderer.bloodMoon()) cir.setReturnValue(new Vec3(.055, .009, .014));
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void jem$ambientLight(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (EventBoundaryRenderer.bloodMoon()) cir.setReturnValue(Math.min(cir.getReturnValue(), .12f));
    }
}
