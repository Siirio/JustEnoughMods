package com.siirio.jemrewards.client;

import java.util.List;
import mezz.jei.api.*;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/** JEI discovers this optional client-only integration through its native annotation. */
@JeiPlugin
public final class RewardsJei implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("jem_rewards", "inventory"); }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InventoryScreen.class,new IGuiContainerHandler<InventoryScreen>() {
            @Override public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
                Rect2i area=RewardsPanel.bounds(screen);return area.getWidth()==0?List.of():List.of(area);
            }
        });
    }
}
