package com.justenoughmods.achievementguide.integration;

import com.tom.storagemod.tile.FilteredInventoryCableConnectorBlockEntity;

/** Identity supplied by the real endpoint's native handler construction. */
public interface TomsFilterEndpointAccess {
    void jem$bindFilterEndpoint(FilteredInventoryCableConnectorBlockEntity endpoint);
}
