package com.siirio.jemsettlements.happiness;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, spatially stratified bounded room sampling and block-light thresholds. */
public final class LightingPolicy {
    public record Point(int x, int y, int z) { }
    public static List<Point> sample(Iterable<Point> floor, int maximum) {
        if (maximum <= 0) return List.of();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point point : floor) {
            minX = Math.min(minX, point.x()); minZ = Math.min(minZ, point.z()); minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x()); maxZ = Math.max(maxZ, point.z()); maxY = Math.max(maxY, point.y());
        }
        if (minX == Integer.MAX_VALUE) return List.of();
        int bands = minY == maxY ? 1 : Math.min(3, maximum), perBand = Math.max(1, maximum / bands);
        int columns = (int) Math.ceil(Math.sqrt(perBand)), rows = Math.max(1, perBand / columns);
        double width = (double) maxX - minX + 1, depth = (double) maxZ - minZ + 1;
        Point[] samples = new Point[maximum]; double[] ranks = new double[maximum];
        java.util.Arrays.fill(ranks, Double.POSITIVE_INFINITY);
        for (Point point : floor) {
            int column = Math.min(columns - 1, (int) ((point.x() - (double) minX) * columns / width));
            int row = Math.min(rows - 1, (int) ((point.z() - (double) minZ) * rows / depth));
            int band = Math.min(bands - 1, (int) ((point.y() - (double) minY) * bands / ((double) maxY - minY + 1)));
            int bucket = Math.min(maximum - 1, band * perBand + Math.min(perBand - 1, column + columns * row));
            double dx = point.x() - (minX + (column + .5) * width / columns), dz = point.z() - (minZ + (row + .5) * depth / rows);
            double targetY = bands == 1 ? minY : minY + band * ((double) maxY - minY) / (bands - 1);
            double rank = Math.abs(point.y() - targetY) * 1_000_000_000D + dx * dx + dz * dz;
            if (rank < ranks[bucket]) { ranks[bucket] = rank; samples[bucket] = point; }
        }
        var result = new ArrayList<Point>();
        for (Point point : samples) if (point != null) result.add(point);
        return List.copyOf(result);
    }
    public static int penalty(int blockLight, int good, int mild, int mildPenalty, int severePenalty) {
        return blockLight >= good ? 0 : blockLight >= Math.min(mild, good) ? mildPenalty : severePenalty;
    }
    private LightingPolicy() { }
}
