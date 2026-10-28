package com.siirio.jeminventoryshortcuts.feature.inventory;

import com.mojang.logging.LogUtils;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;

/** Exact reviewed SophisticatedCore API for physical storage cells only. */
final class SophisticatedStorageDelete {
    private static final String BACKPACK_MENU = "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer";
    private static final String STORAGE_MENU = "net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu";
    private static boolean versionMismatchLogged;

    private SophisticatedStorageDelete() {}

    static boolean allowed(AbstractContainerMenu menu, Slot slot, int menuSlot) {
        String name = menu.getClass().getName();
        if (!version("sophisticatedcore", "1.5.15.2427")
                || (name.equals(BACKPACK_MENU) && !version("sophisticatedbackpacks", "3.26.14.2226"))
                || (name.equals(STORAGE_MENU) && !version("sophisticatedstorage", "1.5.9.2197"))) {
            if (!versionMismatchLogged) {
                versionMismatchLogged = true;
                LogUtils.getLogger().warn("JEM Inventory Shortcuts Delete disabled in {}: installed Sophisticated storage version needs review", name);
            }
            return false;
        }
        if (!(menu instanceof StorageContainerMenuBase<?> storage)
                || !(slot instanceof StorageInventorySlot inventorySlot)
                || !storage.isStorageInventorySlot(menuSlot)) return false;
        var handler = storage.getStorageWrapper().getInventoryHandler();
        return slot.index == menuSlot && inventorySlot.getItemHandler() == handler
                && handler.isSlotAccessible(menuSlot) && !handler.isInfinite(menuSlot) && !inventorySlot.isInfinite();
    }

    private static boolean version(String id, String expected) {
        return ModList.get().getModContainerById(id)
                .map(mod -> expected.equals(mod.getModInfo().getVersion().toString())).orElse(false);
    }
}
