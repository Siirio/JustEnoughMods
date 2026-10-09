package com.siirio.jemserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClaimBoundaryGeometry {
    public record Rectangle(int minX, int minZ, int maxX, int maxZ) {}
    public record Segment(int x1, int z1, int x2, int z2) {}
    public record Mesh(List<Rectangle> fills, List<Segment> edges) {}
    private record Span(int start, int end) {}
    private ClaimBoundaryGeometry() {}

    public static Mesh union(List<Rectangle> rectangles) {
        int[] xs = rectangles.stream().flatMapToInt(rect -> java.util.stream.IntStream.of(rect.minX(), rect.maxX())).distinct().sorted().toArray();
        List<Rectangle> fills = new ArrayList<>();
        List<Segment> edges = new ArrayList<>();
        List<Span> previous = List.of();
        for (int index = 1; index < xs.length; index++) {
            int left = xs[index - 1], right = xs[index];
            List<Span> spans = new ArrayList<>();
            var active = rectangles.stream().filter(rect -> rect.minX() < right && rect.maxX() > left)
                    .sorted(Comparator.comparingInt(Rectangle::minZ)).toList();
            for (Rectangle rect : active) {
                if (spans.isEmpty() || spans.get(spans.size() - 1).end() < rect.minZ()) spans.add(new Span(rect.minZ(), rect.maxZ()));
                else {
                    Span last = spans.remove(spans.size() - 1);
                    spans.add(new Span(last.start(), Math.max(last.end(), rect.maxZ())));
                }
            }
            vertical(edges, left, spans, previous);
            vertical(edges, left, previous, spans);
            for (Span span : spans) {
                fills.add(new Rectangle(left, span.start(), right, span.end()));
                edges.add(new Segment(left, span.start(), right, span.start()));
                edges.add(new Segment(left, span.end(), right, span.end()));
            }
            previous = spans;
        }
        if (xs.length > 0) vertical(edges, xs[xs.length - 1], previous, List.of());
        return new Mesh(List.copyOf(fills), List.copyOf(edges));
    }

    private static void vertical(List<Segment> edges, int x, List<Span> source, List<Span> covered) {
        for (Span span : source) {
            int start = span.start();
            for (Span other : covered) {
                if (other.end() <= start) continue;
                if (other.start() >= span.end()) break;
                if (other.start() > start) edges.add(new Segment(x, start, x, Math.min(span.end(), other.start())));
                start = Math.max(start, other.end());
                if (start >= span.end()) break;
            }
            if (start < span.end()) edges.add(new Segment(x, start, x, span.end()));
        }
    }
}
