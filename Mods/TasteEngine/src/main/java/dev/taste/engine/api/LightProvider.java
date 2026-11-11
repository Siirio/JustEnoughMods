package dev.taste.engine.api;

import java.util.List;

@FunctionalInterface
public interface LightProvider {
    record Query(String dimension, double cameraX, double cameraY, double cameraZ, double radius) { }
    record Light(double x, double y, double z, float red, float green, float blue, float intensity, float range, boolean shadow) { }
    List<Light> lights(Query query);
}
