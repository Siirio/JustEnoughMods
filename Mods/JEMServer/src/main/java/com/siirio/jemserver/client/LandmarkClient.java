package com.siirio.jemserver.client;

import com.siirio.jemserver.ServerData;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class LandmarkClient {
    private LandmarkClient() {}

    public static void register() {
        if (ModList.get().isLoaded("xaerominimap")) {
            MinecraftForge.EVENT_BUS.addListener(XaeroLandmarks::tick);
            MinecraftForge.EVENT_BUS.addListener(XaeroLandmarks::logout);
        }
    }

    public static void accept(List<ServerData.Landmark> landmarks) {
        if (ModList.get().isLoaded("xaerominimap")) XaeroLandmarks.accept(landmarks);
    }
}
