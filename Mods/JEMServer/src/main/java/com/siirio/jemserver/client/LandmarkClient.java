package com.siirio.jemserver.client;

import com.siirio.jemserver.ServerData;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class LandmarkClient {
    private static final java.util.Map<java.util.UUID, ServerData.Landmark> SNAPSHOT = new java.util.LinkedHashMap<>();
    private LandmarkClient() {}

    public static void register() {
        if (ModList.get().isLoaded("xaerominimap")) {
            MinecraftForge.EVENT_BUS.addListener(XaeroLandmarks::tick);
            MinecraftForge.EVENT_BUS.addListener(LandmarkClient::logout);
        }
    }

    public static void accept(List<ServerData.Landmark> landmarks, List<java.util.UUID> removed, boolean reset) {
        if (!ModList.get().isLoaded("xaerominimap")) return;
        if (reset) SNAPSHOT.clear();
        removed.forEach(SNAPSHOT::remove);
        landmarks.forEach(landmark -> SNAPSHOT.put(landmark.id(), landmark));
        XaeroLandmarks.accept(List.copyOf(SNAPSHOT.values()));
    }

    private static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SNAPSHOT.clear();
        XaeroLandmarks.logout(event);
    }
}
