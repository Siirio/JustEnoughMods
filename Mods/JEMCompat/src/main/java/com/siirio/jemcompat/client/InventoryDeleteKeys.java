package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.client.jei.JEMJeiPlugin;
import com.siirio.jemcompat.feature.inventory.InventoryDeleteNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

/** Delete affects only the hovered slot; text editing retains the key. */
@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT)
public final class InventoryDeleteKeys {
    private InventoryDeleteKeys() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_DELETE
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getFocused() instanceof EditBox
                || (ModList.get().isLoaded("jei") && JEMJeiPlugin.hasSearchFocus())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !screen.getMenu().getCarried().isEmpty()) return;
        var slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.isActive() || !slot.hasItem()) return;
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        int slotIndex;
        if (screen instanceof CreativeModeInventoryScreen) {
            // Creative's ItemPickerMenu is client-side and wraps real inventory slots. Send the
            // matching player InventoryMenu index, never an infinite catalog or destroy slot.
            if (menu != minecraft.player.inventoryMenu || slot.container != minecraft.player.getInventory()) return;
            slotIndex = inventorySlot(menu, slot);
        } else {
            if (menu != screen.getMenu()) return;
            slotIndex = menu.slots.indexOf(slot);
        }
        if (slotIndex < 0) return;

        boolean all = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        InventoryDeleteNetwork.request(menu, slotIndex, all, slot.getItem());
        event.setCanceled(true);
    }

    private static int inventorySlot(AbstractContainerMenu menu, Slot visible) {
        ItemStack expected = visible.getItem();
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot actual = menu.getSlot(index);
            if (actual.container == visible.container && actual.getSlotIndex() == visible.getSlotIndex()
                    && actual.getItem().getCount() == expected.getCount()
                    && ItemStack.isSameItemSameTags(actual.getItem(), expected)) return index;
        }
        return -1;
    }
}
