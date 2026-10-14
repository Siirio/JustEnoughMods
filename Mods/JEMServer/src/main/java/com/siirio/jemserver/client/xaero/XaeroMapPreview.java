package com.siirio.jemserver.client.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import xaero.map.WorldMapSession;
import xaero.map.controls.ControlsRegister;
import xaero.map.gui.GuiMap;

public final class XaeroMapPreview {
    private XaeroMapPreview() {}

    public static boolean open(ResourceLocation dimension, BlockPos position) {
        var minecraft = Minecraft.getInstance();
        var session = WorldMapSession.getCurrentSession();
        if (minecraft.player == null || session == null || !session.isUsable()) return false;
        session.getControlsHandler().keyDown(ControlsRegister.keyOpenMap, false, false);
        return minecraft.screen instanceof GuiMap;
    }

}
