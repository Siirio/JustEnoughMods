package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StorageNetworkAccess;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.StorageTerminalBlockEntity", remap = false)
public interface StorageNetworkQuestAccess extends StorageNetworkAccess {
    @Override
    @Accessor(value = "itemHandler", remap = false)
    IItemHandler jem$questStorageNetwork();
}
