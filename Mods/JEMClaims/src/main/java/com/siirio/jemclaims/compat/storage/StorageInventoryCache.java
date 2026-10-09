package com.siirio.jemclaims.compat.storage;

import java.util.Map;
import net.minecraftforge.common.util.LazyOptional;

public interface StorageInventoryCache {
    Map<LazyOptional<?>, LazyOptional<?>> jemclaims$inventoryCache();
}
