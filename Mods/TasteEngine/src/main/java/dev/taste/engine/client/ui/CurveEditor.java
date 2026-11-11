package dev.taste.engine.client.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CurveEditor {
    public record Point(float x, float y) { }
    private final List<Point> points = new ArrayList<>();
    public void set(List<Point> next) { points.clear(); points.addAll(next); points.sort(Comparator.comparing(Point::x)); }
    public float sample(float x) {
        if (points.isEmpty()) return x;
        if (x <= points.get(0).x()) return points.get(0).y();
        for(int i=1;i<points.size();i++){Point b=points.get(i);Point a=points.get(i-1);if(x<=b.x()){float t=(x-a.x())/Math.max(1.0e-6F,b.x()-a.x());t=t*t*(3-2*t);return a.y()+(b.y()-a.y())*t;}}
        return points.get(points.size()-1).y();
    }
    public List<Point> points() { return List.copyOf(points); }
}
