package com.siirio.jemadaptiveculling.client;

import com.siirio.jemadaptiveculling.config.JEMConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CompatibilityPolicy {
    private static final boolean OCULUS = net.minecraftforge.fml.ModList.get().isLoaded("oculus");
    private static final String MINECRAFT = "minecraft";

    private CompatibilityPolicy() {
    }

    public static boolean canCull(Entity entity) {
        if (shadowPass()) return false;
        return switch (JEMConfig.COMPATIBILITY_MODE.get()) {
            case AUTOMATIC -> true;
            case CONSERVATIVE -> minecraft(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
            case NEVER_CULL -> false;
        };
    }

    public static boolean canCull(BlockEntity blockEntity) {
        if (shadowPass()) return false;
        return switch (JEMConfig.COMPATIBILITY_MODE.get()) {
            case AUTOMATIC -> true;
            case CONSERVATIVE -> minecraft(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
            case NEVER_CULL -> false;
        };
    }

    private static boolean shadowPass() {
        return OCULUS && net.irisshaders.iris.api.v0.IrisApi.getInstance().isRenderingShadowPass();
    }

    private static boolean minecraft(ResourceLocation id) {
        return id != null && MINECRAFT.equals(id.getNamespace());
    }
}
