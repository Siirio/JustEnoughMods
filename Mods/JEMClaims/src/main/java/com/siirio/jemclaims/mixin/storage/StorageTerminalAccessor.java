package com.siirio.jemclaims.mixin.storage;

import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StorageTerminalBlockEntity.class, remap = false)
public interface StorageTerminalAccessor {
    @Accessor("itemHandler")
    IItemHandler jemclaims$inventory();
}
