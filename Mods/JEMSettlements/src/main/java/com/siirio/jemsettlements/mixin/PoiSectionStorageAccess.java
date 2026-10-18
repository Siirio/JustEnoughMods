package com.siirio.jemsettlements.mixin;

import java.util.Optional;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionStorage.class)
public interface PoiSectionStorageAccess {
    @Invoker("getOrLoad") Optional<?> jem$section(long position);
}
