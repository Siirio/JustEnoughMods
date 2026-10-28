package com.justenoughmods.achievementguide.integration.events;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public interface HolidayGiftView {
    Optional<CompoundTag> jem$questGift(UUID id);
}
