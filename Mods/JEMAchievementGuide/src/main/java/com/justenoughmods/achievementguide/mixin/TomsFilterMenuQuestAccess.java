package com.justenoughmods.achievementguide.mixin;

import com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.InventoryConnectorFilterMenu", remap = false)
public interface TomsFilterMenuQuestAccess {
    @Accessor(value = "te", remap = false)
    FilteredInventoryCableConnectorBlockEntity jem$nativeFilterEndpoint();
}
