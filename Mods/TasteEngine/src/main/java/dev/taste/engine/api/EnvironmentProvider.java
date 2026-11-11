package dev.taste.engine.api;

import dev.taste.engine.environment.EnvironmentSnapshot;
import java.util.Optional;

@FunctionalInterface
public interface EnvironmentProvider {
    Optional<EnvironmentSnapshot> environment(String dimension, double x, double y, double z);
}
