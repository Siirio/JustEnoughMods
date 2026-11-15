package com.siirio.unofficialoffgrid;

import dev.tazer.offgrid.OffgridTags;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RelativePlacementClassifier {
    private static final TagKey<net.minecraft.world.level.block.Block> DENIED = TagKey.create(
        Registries.BLOCK,
        new ResourceLocation(UnofficialOffgrid.MOD_ID, "relative_placement_denied")
    );
    private static final List<Property<?>> SIDE_CONNECTIONS = List.of(
        BlockStateProperties.NORTH,
        BlockStateProperties.EAST,
        BlockStateProperties.SOUTH,
        BlockStateProperties.WEST,
        BlockStateProperties.NORTH_WALL,
        BlockStateProperties.EAST_WALL,
        BlockStateProperties.SOUTH_WALL,
        BlockStateProperties.WEST_WALL
    );
    private static final Map<BlockState, Boolean> CACHE = new ConcurrentHashMap<>();

    private RelativePlacementClassifier() {
    }

    public static boolean extendsOffgrid(BlockState state) {
        return !state.is(OffgridTags.CAN_BLOCKLOG) && CACHE.computeIfAbsent(state, RelativePlacementClassifier::compute);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean compute(BlockState original) {
        if (original.is(DENIED) || original.hasBlockEntity()) {
            return false;
        }
        if (original.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        if (original.getPistonPushReaction() == PushReaction.BLOCK) {
            return false;
        }

        BlockState state = hasSideConnections(original) ? original.getBlock().defaultBlockState() : original;
        VoxelShape outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (outline.isEmpty()) {
            return false;
        }

        AABB outlineBox = outline.bounds();
        if (outlineBox.maxY > 1.51D || fullDimensions(outlineBox) >= 2) {
            return false;
        }

        VoxelShape collision = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (collision.isEmpty()) {
            return !fillsOverTime(state);
        }

        AABB box = collision.bounds();
        int thinDimensions = 0;
        if (box.getXsize() <= 0.501D) thinDimensions++;
        if (box.getYsize() <= 0.501D) thinDimensions++;
        if (box.getZsize() <= 0.501D) thinDimensions++;
        return thinDimensions >= 2;
    }

    private static boolean hasSideConnections(BlockState state) {
        for (Property<?> property : SIDE_CONNECTIONS) {
            if (state.hasProperty(property)) {
                return true;
            }
        }
        return false;
    }

    private static int fullDimensions(AABB box) {
        int full = 0;
        if (box.getXsize() > 0.876D) full++;
        if (box.getYsize() > 0.876D) full++;
        if (box.getZsize() > 0.876D) full++;
        return full;
    }

    private static boolean fillsOverTime(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty) {
                String name = property.getName();
                if (name.equals("age") || name.endsWith("_amount")) {
                    return true;
                }
            }
        }
        return false;
    }
}
