package com.siirio.jemadaptiveculling.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class ShapeClassifier {
    private static final int MAXIMUM_ENTRIES = 2048;
    private static final Map<VoxelShape, ShapeKind> SHAPES = new IdentityHashMap<>();

    private ShapeClassifier() {
    }

    public static ShapeKind classify(VoxelShape shape) {
        ShapeKind cached = SHAPES.get(shape);
        if (cached != null) {
            return cached;
        }
        ShapeKind kind = shape.isEmpty() ? ShapeKind.EMPTY
                : Block.isShapeFullBlock(shape) ? ShapeKind.FULL : ShapeKind.PARTIAL;
        if (SHAPES.size() >= MAXIMUM_ENTRIES) {
            trimTo(MAXIMUM_ENTRIES / 2);
        }
        SHAPES.put(shape, kind);
        return kind;
    }

    private static void trimTo(int target) {
        Iterator<VoxelShape> iterator = SHAPES.keySet().iterator();
        while (SHAPES.size() > target && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public enum ShapeKind {
        EMPTY,
        FULL,
        PARTIAL
    }
}
