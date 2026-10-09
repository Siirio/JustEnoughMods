package com.siirio.jemserver.client.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

final class XaeroMapPreview {
    private XaeroMapPreview() {}

    static boolean open(ResourceLocation dimension, BlockPos position) {
        var minecraft = Minecraft.getInstance();
        var session = WorldMapSession.getCurrentSession();
        if (minecraft.player == null || session == null || !session.isUsable()) return false;
        var processor = session.getMapProcessor();
        var world = processor.getMapWorld();
        if (world == null) return false;
        var key = ResourceKey.create(Registries.DIMENSION, dimension);
        if (world.getDimension(key) == null) world.createDimensionUnsynced(key);
        world.setFutureDimensionId(key);
        var screen = new GuiMap(minecraft.screen, minecraft.screen, processor, minecraft.player);
        ((MapView) screen).jem$center(position.getX(), position.getZ());
        minecraft.setScreen(screen);
        return true;
    }

}
