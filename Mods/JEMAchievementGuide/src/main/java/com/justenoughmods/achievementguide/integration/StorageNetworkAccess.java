package com.justenoughmods.achievementguide.integration;

import net.minecraftforge.items.IItemHandler;

/** Read-only access to the native terminal's current connected capability. */
public interface StorageNetworkAccess {
    IItemHandler jem$questStorageNetwork();
}
