package com.siirio.jemtwelveeyes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public final class CampaignMapClient {
    private CampaignMapClient() {
    }

    public static void open(int x, int y, int z, ResourceLocation dimension, String name) {
        if (ModList.get().isLoaded("xaeroworldmap") && ModList.get().isLoaded("xaerominimap")) {
            XaeroBossMapScreen.open(x, y, z, dimension, name);
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(name).append(": ")
                    .append(Component.translatable("chat.coordinates", x, y, z))
                    .append(" (" + dimension + ")"), false);
        }
    }
}
