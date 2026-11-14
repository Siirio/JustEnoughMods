package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.LightingSettings;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Unique private static final Vec3 JEM_CHERRY_SKY = Vec3.fromRGB24(0xB687B1);
    @Unique private static final Vec3 JEM_DESERT_SKY = Vec3.fromRGB24(0xEDCA8A);

    @Inject(method = "setBlocksDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"))
    private void jemLightingSandbox$invalidateTerrainLighting(BlockPos position, BlockState previous,
                                                              BlockState current, CallbackInfo callback) {
        TerrainLighting.invalidate(position);
    }

    @ModifyArg(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"),
            index = 1)
    private CubicSampler.Vec3Fetcher jemLightingSandbox$sampleSkyPalette(CubicSampler.Vec3Fetcher original) {
        if (!LightingSettings.atmosphereEnabled() || TerrainLighting.isShaderPackActive()) {
            return original;
        }

        ClientLevel level = (ClientLevel) (Object) this;
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (client.level != level || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()
                || !camera.isInitialized() || camera.getFluidInCamera() != FogType.NONE
                || (camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS)))) {
            return original;
        }

        var biomeManager = level.getBiomeManager();
        return (x, y, z) -> {
            var biome = biomeManager.getNoiseBiomeAtQuart(x, y, z);
            if (biome.is(Biomes.CHERRY_GROVE)) {
                return JEM_CHERRY_SKY;
            }
            if (biome.is(Biomes.DESERT)) {
                return JEM_DESERT_SKY;
            }
            return Vec3.fromRGB24(biome.value().getSkyColor());
        };
    }
}
