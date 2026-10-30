package com.siirio.jemadaptiveculling.client;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShapeClassifier {
    private static final int MAXIMUM_ENTRIES = 2048;
    private static final Reference2ObjectLinkedOpenHashMap<VoxelShape, ShapeInfo> SHAPES = new Reference2ObjectLinkedOpenHashMap<>();

    private ShapeClassifier() {
    }

    public static void clear() {
        SHAPES.clear();
    }

    public static ShapeKind classify(VoxelShape shape) {
        return info(shape).kind;
    }

    public static List<AABB> boxes(VoxelShape shape) {
        ShapeInfo info = info(shape);
        if (info.boxes == null) {
            List<AABB> boxes = shape.toAabbs();
            info.boxes = boxes.size() > 32 ? List.of() : List.copyOf(boxes);
        }
        return info.boxes;
    }

    private static ShapeInfo info(VoxelShape shape) {
        ShapeInfo cached = SHAPES.getAndMoveToLast(shape);
        if (cached != null) return cached;
        ShapeKind kind = shape.isEmpty() ? ShapeKind.EMPTY
                : Block.isShapeFullBlock(shape) ? ShapeKind.FULL : ShapeKind.PARTIAL;
        if (SHAPES.size() >= MAXIMUM_ENTRIES) SHAPES.removeFirst();
        ShapeInfo info = new ShapeInfo(kind);
        SHAPES.putAndMoveToLast(shape, info);
        return info;
    }

    public enum ShapeKind {
        EMPTY,
        FULL,
        PARTIAL
    }

    private static final class ShapeInfo {
        private final ShapeKind kind;
        private List<AABB> boxes;

        private ShapeInfo(ShapeKind kind) {
            this.kind = kind;
        }
    }
}
