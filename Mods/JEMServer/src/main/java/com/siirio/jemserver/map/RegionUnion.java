package com.siirio.jemserver.map;

import java.util.ArrayList;
import java.util.List;

public final class RegionUnion {
    private RegionUnion() {
    }

    public static long area(List<RegionGeometry.Rectangle> rectangles) {
        return RegionGeometry.union(rectangles).fills().stream().mapToLong(rectangle ->
                (long) (rectangle.maxX() - rectangle.minX()) * (rectangle.maxZ() - rectangle.minZ())).sum();
    }

    public static List<List<RegionGeometry.Rectangle>> components(List<RegionGeometry.Rectangle> rectangles) {
        List<List<RegionGeometry.Rectangle>> result = new ArrayList<>();
        for (var rectangle : rectangles) {
            List<List<RegionGeometry.Rectangle>> touched = result.stream()
                    .filter(component -> component.stream().anyMatch(existing -> positiveOverlap(existing, rectangle))).toList();
            List<RegionGeometry.Rectangle> merged = new ArrayList<>();
            touched.forEach(merged::addAll);
            result.removeAll(touched);
            merged.add(rectangle);
            result.add(merged);
        }
        return result.stream().map(List::copyOf).toList();
    }

    private static boolean positiveOverlap(RegionGeometry.Rectangle first, RegionGeometry.Rectangle second) {
        return Math.min(first.maxX(), second.maxX()) > Math.max(first.minX(), second.minX())
                && Math.min(first.maxZ(), second.maxZ()) > Math.max(first.minZ(), second.minZ());
    }
}
