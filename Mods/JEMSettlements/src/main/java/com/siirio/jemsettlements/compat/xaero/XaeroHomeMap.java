package com.siirio.jemsettlements.compat.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class XaeroHomeMap {
    public record Destination(ResourceLocation dimension, BlockPos pos) { }
    private static Destination pending;
    private XaeroHomeMap() { }
    public static void open(Screen parent, CompoundTag view) {
        ResourceLocation dimension = ResourceLocation.tryParse(view.getString("Dimension")); if (dimension == null || !view.contains("Anchor")) return;
        var session = xaero.map.WorldMapSession.getCurrentSession(); var mc = Minecraft.getInstance(); if (session == null || !session.isUsable() || mc.player == null) return;
        pending = new Destination(dimension, BlockPos.of(view.getLong("Anchor"))); mc.setScreen(new xaero.map.gui.GuiMap(parent, parent, session.getMapProcessor(), mc.player));
    }
    public static Destination consume() { Destination value = pending; pending = null; return value; }
}
