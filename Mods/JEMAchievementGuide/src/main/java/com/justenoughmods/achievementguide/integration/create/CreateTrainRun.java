package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** One native train trip and its authenticated schedule assignment. */
public interface CreateTrainRun {
    void jem$scheduleActor(UUID actor);
    UUID jem$scheduleOperator();
    boolean jem$cargoReady(com.simibubi.create.content.trains.entity.Carriage carriage);
    void jem$cargoDelivered(com.simibubi.create.content.trains.entity.Carriage carriage,
                            net.minecraft.world.item.ItemStack item, int versionBefore);
    void jem$writeTrip(CompoundTag tag);
    void jem$readTrip(CompoundTag tag);
}
