package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.events.HolidayGiftView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.siirio.jemserver.smp.events.calendar.CalendarEventData", remap = false)
public interface HolidayGiftAccessMixin extends HolidayGiftView {
    @Override @Invoker("gift") Optional<CompoundTag> jem$questGift(UUID id);
}
