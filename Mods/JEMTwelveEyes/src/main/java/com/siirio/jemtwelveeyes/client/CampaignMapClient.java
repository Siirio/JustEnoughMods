package com.siirio.jemtwelveeyes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public final class CampaignMapClient {
    private CampaignMapClient() {
    }

    public static void open(int x, int y, int z, ResourceLocation dimension, String name, boolean shared) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(name).append(": ")
                    .append(Component.translatable("chat.coordinates", x, y, z))
                    .append(" (" + dimension + ")"), false);
        }
        if (ModList.get().isLoaded("xaeroworldmap")) {
            XaeroMapAdapter.open();
        }
    }
}
