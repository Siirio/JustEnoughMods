package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

/** Actual paired native interface ownership, without a spatial player lookup. */
public interface CreateCargoConnection {
    CarriageContraptionEntity jem$connectedCarriage();
}
