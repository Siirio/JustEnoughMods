package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.client.jei.JEMJeiPlugin;
import com.siirio.jemcompat.feature.inventory.InventoryDeleteNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_DELETE
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getFocused() instanceof EditBox
                || (ModList.get().isLoaded("jei") && JEMJeiPlugin.hasSearchFocus())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.containerMenu != screen.getMenu()
                || !screen.getMenu().getCarried().isEmpty()) return;
        var slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.isActive() || !slot.hasItem()) return;
        int slotIndex = screen.getMenu().slots.indexOf(slot);
        if (slotIndex < 0) return;

        boolean all = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        InventoryDeleteNetwork.request(screen.getMenu(), slotIndex, all, slot.getItem());
        event.setCanceled(true);
    }
}
