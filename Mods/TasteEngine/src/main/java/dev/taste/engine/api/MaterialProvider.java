package dev.taste.engine.api;

import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import java.util.Optional;

@FunctionalInterface
public interface MaterialProvider {
    Optional<MaterialDescription> material(MaterialQuery query);
}
