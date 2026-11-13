package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.terrain.WaterMaterial;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials", remap = false)
public abstract class EmbeddiumWaterMaterialMixin {
    @Inject(method = "forFluidState(Lnet/minecraft/world/level/material/FluidState;)Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void jemLightingSandbox$markWater(FluidState state, CallbackInfoReturnable<Material> callback) {
        if (state.is(FluidTags.WATER)) {
            callback.setReturnValue(WaterMaterial.flagged(callback.getReturnValue()));
        }
    }
}
