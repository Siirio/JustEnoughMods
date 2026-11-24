package com.siirio.jematmosphere.mixin;

import com.siirio.jematmosphere.AtmosphereEnvironment;
import com.siirio.jematmosphere.AtmosphereSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "com.qendolin.betterclouds.clouds.EffectTintProvider", remap = false)
public abstract class BetterCloudsTintMixin {
    @ModifyArg(method = "getEffectTint(Lnet/minecraft/client/Minecraft;"
                    + "Lcom/qendolin/betterclouds/clouds/fog/FogProvider$Fog;F)Lorg/joml/Vector3f;",
            at = @At(value = "INVOKE", target = "Lcom/qendolin/betterclouds/clouds/EffectTintProvider;"
                    + "compositeColor(Lorg/joml/Vector3f;Lorg/joml/Vector4f;)V"),
            index = 0, remap = false)
    private static Vector3f jemAtmosphere$neutralCloudFog(Vector3f fog) {
        if (!AtmosphereSettings.skyColorsEnabled() || AtmosphereEnvironment.isShaderPackActive()) {
            return fog;
        }

        var client = Minecraft.getInstance();
        var level = client.level;
        var camera = client.gameRenderer.getMainCamera();
        if (level == null || !camera.isInitialized() || camera.getFluidInCamera() != FogType.NONE
                || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()
                || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || level.getSkyFlashTime() > 0 || client.gui.getBossOverlay().shouldCreateWorldFog()
                || (camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS)))) {
            return fog;
        }

        var position = camera.getBlockPosition();
        if (level.effects().isFoggyAt(position.getX(), position.getZ())) {
            return fog;
        }
        var biome = level.getBiome(position);
        if (!biome.is(Biomes.PLAINS) && !biome.is(Biomes.SUNFLOWER_PLAINS)
                && !biome.is(Biomes.BIRCH_FOREST) && !biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return fog;
        }

        float clear = 1F - level.getRainLevel(client.getFrameTime());
        if (clear <= 0F) {
            return fog;
        }

        double luminance = 0.299 * Math.pow(fog.x, 2.2) + 0.587 * Math.pow(fog.y, 2.2)
                + 0.114 * Math.pow(fog.z, 2.2);
        float gray = (float) Math.pow(luminance, 1.0 / 2.2);
        return fog.lerp(new Vector3f(gray), clear);
    }
}
