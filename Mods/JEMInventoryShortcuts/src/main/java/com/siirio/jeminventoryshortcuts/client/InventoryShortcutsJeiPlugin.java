package com.siirio.jeminventoryshortcuts.client;

import com.siirio.jeminventoryshortcuts.JEMInventoryShortcuts;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** Optional JEI focus bridge so Delete remains available for editing search text. */
@JeiPlugin
public final class InventoryShortcutsJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(JEMInventoryShortcuts.MOD_ID, "delete_key_guard");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime availableRuntime) {
        runtime = availableRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    static boolean hasSearchFocus() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }
}
