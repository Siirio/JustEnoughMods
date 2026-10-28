package com.justenoughmods.achievementguide.integration.travel;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Called only after the vanilla authenticated vehicle packet has passed collision validation. */
public interface TravelVehicleObservation {
    void jem$travelAccepted(ServerPlayer actor, Vec3 from, boolean groundedBefore);
    void jem$travelReset();
}
