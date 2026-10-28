package com.justenoughmods.achievementguide.mixin;

import java.util.Map;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart", remap = false)
public interface CompressionPartQuestAccess {
    @Accessor(value = "slotDefinitions", remap = false) Map<Integer, ?> jem$nativeCompressionDefinitions();
    @Accessor(value = "slotRange", remap = false) SlotRange jem$nativeCompressionRange();
}
