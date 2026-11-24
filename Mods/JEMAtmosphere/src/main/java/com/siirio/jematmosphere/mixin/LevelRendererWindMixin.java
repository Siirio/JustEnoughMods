package com.siirio.jematmosphere.mixin;

import com.siirio.jematmosphere.AtmosphereSettings;
import com.siirio.jematmosphere.WindGusts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererWindMixin {
    @ModifyVariable(method = "renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
            at = @At("STORE"), index = 9)
    private float jemAtmosphere$renderWind(float rain, LightTexture lightTexture, float partialTick,
            double cameraX, double cameraY, double cameraZ) {
        if (AtmosphereSettings.windEnabled()) {
            WindGusts.draw(Minecraft.getInstance(), new Vec3(cameraX, cameraY, cameraZ), partialTick);
        }
        return rain;
    }
}
