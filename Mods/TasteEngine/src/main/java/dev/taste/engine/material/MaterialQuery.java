package dev.taste.engine.material;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

/** Stable identifiers only. World and renderer objects do not escape into material caches. */
public record MaterialQuery(ResourceLocation block, ResourceLocation fluid, ResourceLocation sprite,
        Set<ResourceLocation> tags, boolean translucent, boolean cutout, int tintIndex,
        Set<ResourceLocation> fluidTags, ResourceLocation entity, Set<ResourceLocation> entityTags,
        Map<String, String> state, Layer layer) {
    public enum Layer {
        SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT, ENTITY, PARTICLE
    }

    public MaterialQuery {
        tags = Set.copyOf(tags);
        fluidTags = Set.copyOf(fluidTags);
        entityTags = Set.copyOf(entityTags);
        state = Map.copyOf(state);
    }

    public MaterialQuery(ResourceLocation block, ResourceLocation fluid, ResourceLocation sprite,
            Set<ResourceLocation> tags, boolean translucent, boolean cutout, int tintIndex) {
        this(block, fluid, sprite, tags, translucent, cutout, tintIndex, Set.of(), null, Set.of(),
                Map.of(), translucent ? Layer.TRANSLUCENT : cutout ? Layer.CUTOUT : null);
    }

    public static MaterialQuery block(BlockState state, ResourceLocation sprite, Layer layer, int tintIndex) {
        Map<String, String> values = new HashMap<>();
        for (Property<?> property : state.getProperties()) {
            values.put(property.getName(), valueName(state, property));
        }
        return new MaterialQuery(BuiltInRegistries.BLOCK.getKey(state.getBlock()), null, sprite,
                locations(state.getTags()), layer == Layer.TRANSLUCENT, isCutout(layer), tintIndex,
                Set.of(), null, Set.of(), values, layer);
    }

    public static MaterialQuery fluid(FluidState state, ResourceLocation sprite, Layer layer, int tintIndex) {
        return new MaterialQuery(null, state.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(state.getType()),
                sprite, Set.of(), layer == Layer.TRANSLUCENT, isCutout(layer), tintIndex,
                locations(state.getTags()), null, Set.of(), Map.of(), layer);
    }

    public static MaterialQuery entity(EntityType<?> type, ResourceLocation sprite, Layer layer) {
        return new MaterialQuery(null, null, sprite, Set.of(), layer == Layer.TRANSLUCENT, isCutout(layer), -1,
                Set.of(), BuiltInRegistries.ENTITY_TYPE.getKey(type), locations(type.getTags()), Map.of(), layer);
    }

    private static boolean isCutout(Layer layer) {
        return layer == Layer.CUTOUT || layer == Layer.CUTOUT_MIPPED;
    }

    private static <T> Set<ResourceLocation> locations(Stream<TagKey<T>> tags) {
        return tags.map(TagKey::location).collect(Collectors.toUnmodifiableSet());
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
