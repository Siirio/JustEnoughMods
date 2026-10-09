package com.siirio.jemcompat.mixin.worldtier;

import com.siirio.jemcompat.feature.worldtier.EnhancedAiTierPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "insane96mcp.enhancedai.modules.mobs.Spawning", remap = false)
public abstract class EnhancedAiSpawningMixin {
    @Inject(method = "isUnaffectedByFeatures", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemcompat$excludeScriptedNamespaces(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (EnhancedAiTierPolicy.excludedNamespace(entityId.getNamespace())) {
            callback.setReturnValue(true);
        }
    }
}
