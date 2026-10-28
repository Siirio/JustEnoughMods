package com.justenoughmods.achievementguide.integration.travel;

import net.minecraft.world.phys.Vec3;

/** Positive native thermal impulse followed by an actually accepted altitude gain. */
public interface TravelThermalObservation {
    void jem$thermalAccepted(Vec3 from);
}
