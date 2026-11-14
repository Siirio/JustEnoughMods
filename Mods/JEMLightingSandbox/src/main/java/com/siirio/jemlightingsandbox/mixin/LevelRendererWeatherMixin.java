package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.PrecipitationRenderer;
import com.siirio.jemlightingsandbox.WindGusts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererWeatherMixin {
    @ModifyVariable(
            method = "renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
            at = @At("STORE"),
            index = 9)
    private float jem$renderWindDrivenPrecipitation(float rain, LightTexture lightTexture, float partialTick,
            double cameraX, double cameraY, double cameraZ) {
        var client = Minecraft.getInstance();
        if (!PrecipitationRenderer.replacesVanilla(client)) {
            return rain;
        }
        var camera = new Vec3(cameraX, cameraY, cameraZ);
        PrecipitationRenderer.draw(client, camera, partialTick, rain);
        WindGusts.draw(client, camera, partialTick);
        return 0F;
    }
}
