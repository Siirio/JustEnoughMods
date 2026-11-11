package dev.taste.engine.api.instances;

import java.util.List;

@FunctionalInterface
public interface InstanceProvider {
    record Query(String dimension, long frame, double originX, double originY, double originZ, double radius) { }
    record Instance(long id, String material, float[] transform, float[] previousTransform, int geometryHandle) {
        public Instance { transform = transform.clone(); previousTransform = previousTransform.clone(); }
    }
    List<Instance> instances(Query query);
}
