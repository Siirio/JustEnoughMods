package com.siirio.jemadaptiveculling.client;

import com.siirio.jemadaptiveculling.config.JEMConfig;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public final class CompatibilityPolicy {
    private static final boolean OCULUS = ModList.get().isLoaded("oculus");
    private static final boolean EXTERNAL_OCCLUSION = ModList.get().isLoaded("entityculling");
    private static final String MINECRAFT = "minecraft";
    private static final Map<Block, Boolean> OCCLUDERS = new IdentityHashMap<>();
    private static Set<ResourceLocation> excludedEntities;
    private static Set<ResourceLocation> excludedBlockEntities;
    private static Set<ResourceLocation> excludedOccluders;

    private CompatibilityPolicy() {
    }

    public static void reload() {
        excludedEntities = identifiers(JEMConfig.EXCLUDED_ENTITIES.get());
        excludedBlockEntities = identifiers(JEMConfig.EXCLUDED_BLOCK_ENTITIES.get());
        excludedOccluders = identifiers(JEMConfig.EXCLUDED_OCCLUDERS.get());
        OCCLUDERS.clear();
    }

    public static boolean canCull(Entity entity) {
        if (EXTERNAL_OCCLUSION || shadowPass()) return false;
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (excludedEntities.contains(id)) return false;
        return switch (JEMConfig.COMPATIBILITY_MODE.get()) {
            case AUTOMATIC -> true;
            case CONSERVATIVE -> minecraft(id);
            case NEVER_CULL -> false;
        };
    }

    public static boolean canCull(BlockEntity blockEntity) {
        if (EXTERNAL_OCCLUSION || shadowPass()) return false;
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (excludedBlockEntities.contains(id)) return false;
        return switch (JEMConfig.COMPATIBILITY_MODE.get()) {
            case AUTOMATIC -> true;
            case CONSERVATIVE -> minecraft(id);
            case NEVER_CULL -> false;
        };
    }

    public static boolean canOcclude(BlockState state) {
        if (!state.canOcclude() || state.getBlock().hasDynamicShape() || state.getRenderShape() != RenderShape.MODEL) return false;
        ensureLoaded();
        Block block = state.getBlock();
        Boolean allowed = OCCLUDERS.get(block);
        if (allowed == null) {
            allowed = !excludedOccluders.contains(BuiltInRegistries.BLOCK.getKey(block));
            OCCLUDERS.put(block, allowed);
        }
        return allowed;
    }

    private static void ensureLoaded() {
        if (excludedEntities == null) reload();
    }

    private static Set<ResourceLocation> identifiers(List<? extends String> values) {
        Set<ResourceLocation> result = new HashSet<>();
        for (String value : values) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null) result.add(id);
        }
        return result;
    }

    private static boolean shadowPass() {
        return OCULUS && net.irisshaders.iris.api.v0.IrisApi.getInstance().isRenderingShadowPass();
    }

    private static boolean minecraft(ResourceLocation id) {
        return id != null && MINECRAFT.equals(id.getNamespace());
    }
}
