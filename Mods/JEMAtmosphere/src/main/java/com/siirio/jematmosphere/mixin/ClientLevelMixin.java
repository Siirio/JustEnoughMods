package com.siirio.jematmosphere.mixin;

import com.siirio.jematmosphere.AtmosphereEnvironment;
import com.siirio.jematmosphere.AtmospherePalette;
import com.siirio.jematmosphere.AtmosphereSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @ModifyArg(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"),
            index = 1)
    private CubicSampler.Vec3Fetcher jemAtmosphere$sampleSkyPalette(CubicSampler.Vec3Fetcher original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!jemAtmosphere$canTintSky(level)) {
            return original;
        }
        var biomeManager = level.getBiomeManager();
        return (x, y, z) -> AtmospherePalette.clearSky(biomeManager.getNoiseBiomeAtQuart(x, y, z));
    }

    @Inject(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void jemAtmosphere$tintWeatherSky(Vec3 position, float partialTick,
            CallbackInfoReturnable<Vec3> callback) {
        ClientLevel level = (ClientLevel) (Object) this;
        float rain = level.getRainLevel(partialTick);
        if (rain <= 0F || level.getSkyFlashTime() > 0 || !jemAtmosphere$canTintSky(level)) {
            return;
        }
        float daylight = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2F + 0.5F, 0F, 1F);
        callback.setReturnValue(AtmospherePalette.weatherSky(callback.getReturnValue(), rain,
                level.getThunderLevel(partialTick), daylight));
    }

    @Unique
    private static boolean jemAtmosphere$canTintSky(ClientLevel level) {
        if (!AtmosphereSettings.skyColorsEnabled() || AtmosphereEnvironment.isShaderPackActive()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level != level || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            return false;
        }
        Camera camera = client.gameRenderer.getMainCamera();
        return camera.isInitialized() && camera.getFluidInCamera() == FogType.NONE
                && !(camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS)));
    }
}
